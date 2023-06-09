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

package io.github.fisher2911.hmcleaves.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public record Position(UUID world, int x, int y, int z) {

    public static Position fromLocation(Location location) throws IllegalStateException {
        if (location.getWorld() == null) throw new IllegalStateException("Location must have a world!");
        return at(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static Position at(UUID world, int x, int y, int z) {
        return new Position(world, x, y, z);
    }

    public Position add(int x, int y, int z) {
        return new Position(this.world, this.x + x, this.y + y, this.z + z);
    }

    public Position add(Position position) {
        return add(position.x, position.y, position.z);
    }

    public Position subtract(int x, int y, int z) {
        return new Position(this.world, this.x - x, this.y - y, this.z - z);
    }

    public Position subtract(Position position) {
        return subtract(position.x, position.y, position.z);
    }

    public Location toLocation() throws IllegalStateException {
        final World world = Bukkit.getWorld(this.world);
        if (world == null) throw new IllegalStateException("World " + this.world + " is not loaded!");
        return new Location(world, this.x, this.y, this.z);
    }

    public ChunkPosition getChunkPosition() {
        return new ChunkPosition(this.world, this.x >> 4, this.z >> 4);
    }

    @Override
    public String toString() {
        return "Position{" +
                "world=" + world +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

}
