/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.listener;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.data.BlockDataSound;
import io.github.fisher2911.hmcleaves.data.SoundData;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;

import java.util.List;

public class SoundListener implements Listener {

    private final HMCLeaves plugin;
    private final BlockCache cache;

    public SoundListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.cache = this.plugin.getBlockCache();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onGameEvent(GenericGameEvent event) {
        final GameEvent gameEvent = event.getEvent();
        if (gameEvent == GameEvent.HIT_GROUND) {
            if (!(event.getEntity() instanceof final Player player)) return;
            final Position position = Position.fromLocation(event.getLocation().clone().subtract(0, 1, 0));
            final BlockDataSound sound = this.cache.getBlockData(position).blockDataSound();
            if (sound == null) return;
            final SoundData hitSound = sound.hitSound();
            if (hitSound == null) return;
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
                    PacketUtils.sendSound(hitSound, position, List.of(player))
            );
            return;
        }
        if (gameEvent == GameEvent.STEP) {
            if (!(event.getEntity() instanceof final Player player)) return;
            final Position position = Position.fromLocation(event.getLocation().clone().subtract(0, 1, 0));
            final BlockDataSound sound = this.cache.getBlockData(position).blockDataSound();
            if (sound == null) return;
            final SoundData stepSound = sound.stepSound();
            if (stepSound == null) return;
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
                    PacketUtils.sendSound(stepSound, position, List.of(player))
            );
            return;
        }
    }

}
