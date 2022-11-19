package io.github.fisher2911.hmcleaves.util;

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
