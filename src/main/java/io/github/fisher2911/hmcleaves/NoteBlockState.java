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

package io.github.fisher2911.hmcleaves;

import org.bukkit.Instrument;

import java.util.Locale;
import java.util.Objects;

public record NoteBlockState(Instrument instrument, byte note) {

    @Override
    public String toString() {
        return this.instrument.name().toLowerCase(Locale.ROOT) + ":" + this.note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NoteBlockState that = (NoteBlockState) o;
        return note == that.note && instrument == that.instrument;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrument, note);
    }

}
