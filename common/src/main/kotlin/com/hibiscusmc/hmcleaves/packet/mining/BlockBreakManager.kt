package com.hibiscusmc.hmcleaves.packet.mining

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.packet.removeMiningFatigue
import com.hibiscusmc.hmcleaves.packet.sendBlockBreakAnimation
import com.hibiscusmc.hmcleaves.packet.sendBlockBroken
import com.hibiscusmc.hmcleaves.world.Position
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow


class BlockBreakManager(
    private val blockBreakDataMap: MutableMap<UUID, BlockBreakData> = ConcurrentHashMap(),
    private val plugin: HMCLeaves,
    private val worldManager: WorldManager = plugin.worldManager,
    private val config: LeavesConfig = plugin.leavesConfig
) {
    private val randomRange = (10_000..20_000)

    fun getBlockBreakData(uuid: UUID): BlockBreakData? {
        return blockBreakDataMap[uuid]
    }

    fun startBlockBreak(
        player: Player,
        position: Position,
        blockData: BlockData
    ) {
        if (blockData.blockBreakModifier == null) return
        val blockBreakTime = this.calculateBlockBreakTimeInTicks(
            player,
            player.inventory.itemInMainHand,
            blockData.blockBreakModifier
        )
        val blockBreakData = BlockBreakData(
            randomRange.random(),
            blockData,
            player,
            position,
            blockBreakTime,
            0.0,
            this.createScheduler(blockBreakTime, player.uniqueId)
        )
        blockBreakDataMap[player.uniqueId] = blockBreakData
    }

    fun cancelBlockBreak(player: Player) {
        val blockBreakData = blockBreakDataMap[player.uniqueId] ?: return
        sendBlockBreakAnimation(
            player,
            blockBreakData.position,
            blockBreakData.entityId,
            (-1).toByte()
        )
        blockBreakData.breakTask.cancel()
        blockBreakDataMap.remove(player.uniqueId)
    }

    private fun createScheduler(blockBreakTime: Int, uuid: UUID): BukkitTask {
        return Bukkit.getScheduler().runTaskTimer(
            this.plugin,
            Runnable {
                val blockBreakData = blockBreakDataMap[uuid] ?: return@Runnable
                val player = blockBreakData.breaker
                if (blockBreakData.isBroken) {
                    val position = blockBreakData.position
                    val block: Block = position.toLocation().block
                    val event = BlockBreakEvent(block, player)
                    Bukkit.getPluginManager().callEvent(event)
                    if (event.isCancelled) {
                        blockBreakData.resetProgress()
                        return@Runnable
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(this.plugin, Runnable {
                        sendBlockBreakAnimation(
                            blockBreakData.breaker,
                            position,
                            blockBreakData.entityId,
                            (-1).toByte()
                        )
                        sendBlockBroken(
                            player,
                            position,
                            blockBreakData.blockData.getBlockGlobalId()
                        )
                    })
                    worldManager[position.world]
                        ?.get(position.getChunkPosition())
                        ?.remove(position.toPositionInChunk(), false)
                    block.type = Material.AIR
                    val blockBreakModifier = blockBreakData.blockData.blockBreakModifier!!
                    val heldItem: ItemStack = blockBreakData.breaker.inventory.itemInMainHand
                    val dropItems = blockBreakModifier.hasEnchantment(heldItem) &&
                            (!blockBreakModifier.requiresToolToDrop ||
                                    blockBreakModifier.hasToolType(heldItem.type))

                    if (event.isDropItems && dropItems) {
                        val world: World = block.world
                        Bukkit.getScheduler().runTaskLater(
                            this@BlockBreakManager.plugin,
                            Runnable {
                                for (itemStack in blockBreakData.blockData.getDrops(config)) {
                                    world.dropItem(
                                        block.location.clone().add(0.5, 0.0, 0.5),
                                        itemStack
                                    )
                                }
                            }, 1
                        )

                    }
                    removeMiningFatigue(player);
                    blockBreakData.breakTask.cancel()
                    blockBreakDataMap.remove(uuid)
                    return@Runnable
                }
                val updatedBlockBreakTime =
                    calculateBlockBreakTimeInTicks(
                        player,
                        player.inventory.itemInMainHand,
                        blockBreakData.blockData.blockBreakModifier!!
                    )
                val percentageToAdd = blockBreakTime.toDouble() / updatedBlockBreakTime
                blockBreakData.addBreakTimeProgress(percentageToAdd)
                blockBreakData.send(blockBreakData.position)
            }, 1, 1
        )
    }

    // formula found at https://minecraft.fandom.com/wiki/Breaking#Speed
    private fun calculateBlockBreakTimeInTicks(player: Player, inHand: ItemStack, modifier: BlockBreakModifier): Int {
        val material: Material = inHand.type
        var speedMultiplier = TOOL_SPEED_MULTIPLIERS.getOrDefault(material, 1).toDouble()
        if (!modifier.hasToolType(material)) {
            speedMultiplier = 1.0
        }
        val efficiencyLevel: Int = inHand.getEnchantmentLevel(Enchantment.DIG_SPEED)
        if (efficiencyLevel != 0) {
            speedMultiplier += efficiencyLevel.toDouble().pow(2.0) + 1
        }
        val haste = player.getPotionEffect(PotionEffectType.FAST_DIGGING)
        if (haste != null) {
            speedMultiplier *= 0.2 * haste.amplifier + 1
        }
        val miningFatigue = player.getPotionEffect(PotionEffectType.SLOW_DIGGING)
        if (miningFatigue != null) {
            speedMultiplier *= 0.3.pow(min(miningFatigue.amplifier.toDouble(), 4.0))
        }
        val helmet: ItemStack? = player.inventory.helmet
        if (player.isInWater) {
            if (helmet == null || helmet.getEnchantmentLevel(Enchantment.WATER_WORKER) <= 0) {
                speedMultiplier /= 5.0
            }
        }
        if (!player.isOnGround) {
            speedMultiplier /= 5.0
        }
        val damage: Double = (speedMultiplier / modifier.hardness) / 30
        if (damage > 1) {
            return 0
        }
        return ceil(1 / damage).toInt()
    }

    class BlockBreakData(
        val entityId: Int,
        val blockData: BlockData,
        val breaker: Player,
        val position: Position,
        private val totalBreakTime: Int,
        private var breakTimeProgress: Double,
        val breakTask: BukkitTask
    ) {
        fun addBreakTimeProgress(breakTimeProgress: Double) {
            this.breakTimeProgress = min(
                this.breakTimeProgress + breakTimeProgress,
                totalBreakTime.toDouble()
            )
        }

        private fun calculateDamage(): Byte {
            val percentage = breakTimeProgress / this.totalBreakTime
            val damage = MAX_DAMAGE * percentage
            return (min(damage, MAX_DAMAGE.toDouble()) - 1).toInt().toByte()
        }

        fun send(position: Position) {
            val calculatedDamage = this.calculateDamage()
            sendBlockBreakAnimation(this.breaker, position, this.entityId, calculatedDamage)
        }

        val isBroken: Boolean
            get() = this.breakTimeProgress >= this.totalBreakTime

        fun resetProgress() {
            this.breakTimeProgress = 0.0
        }

        companion object {
            const val MAX_DAMAGE: Byte = 10
        }
    }

    companion object {

        private const val LOG_HARDNESS = 2.0
        val LOG_BREAK_MODIFIER: BlockBreakModifier = BlockBreakModifier(
            LOG_HARDNESS,
            false,
            mutableSetOf(ToolType.AXE),
            setOf()
        )

        private val TOOL_SPEED_MULTIPLIERS = createToolSpeedModifiers()

        private fun createToolSpeedModifiers(): Map<Material, Int> {
            val map: MutableMap<Material, Int> = EnumMap(org.bukkit.Material::class.java)
            val goldTools = EnumSet.of(
                Material.GOLDEN_SHOVEL,
                Material.GOLDEN_PICKAXE,
                Material.GOLDEN_AXE,
                Material.GOLDEN_HOE,
                Material.GOLDEN_SWORD
            )
            val netheriteTools = EnumSet.of(
                Material.NETHERITE_SHOVEL,
                Material.NETHERITE_PICKAXE,
                Material.NETHERITE_AXE,
                Material.NETHERITE_HOE,
                Material.NETHERITE_SWORD
            )
            val diamondTools = EnumSet.of(
                Material.DIAMOND_SHOVEL,
                Material.DIAMOND_PICKAXE,
                Material.DIAMOND_AXE,
                Material.DIAMOND_HOE,
                Material.DIAMOND_SWORD
            )
            val ironTools = EnumSet.of(
                Material.IRON_SHOVEL,
                Material.IRON_PICKAXE,
                Material.IRON_AXE,
                Material.IRON_HOE,
                Material.IRON_SWORD
            )
            val stoneTools = EnumSet.of(
                Material.STONE_SHOVEL,
                Material.STONE_PICKAXE,
                Material.STONE_AXE,
                Material.STONE_HOE,
                Material.STONE_SWORD
            )
            val woodenTools = EnumSet.of(
                Material.WOODEN_SHOVEL,
                Material.WOODEN_PICKAXE,
                Material.WOODEN_AXE,
                Material.WOODEN_HOE,
                Material.WOODEN_SWORD
            )
            goldTools.forEach(Consumer { material: Material -> map[material] = 12 })
            netheriteTools.forEach(Consumer { material: Material -> map[material] = 9 })
            diamondTools.forEach(Consumer { material: Material -> map[material] = 8 })
            ironTools.forEach(Consumer { material: Material -> map[material] = 6 })
            stoneTools.forEach(Consumer { material: Material -> map[material] = 4 })
            woodenTools.forEach(Consumer { material: Material -> map[material] = 2 })
            map[Material.SHEARS] = 2
            return map
        }
    }
}