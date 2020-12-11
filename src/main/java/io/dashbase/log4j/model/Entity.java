package io.dashbase.log4j.model;

public class Entity {
    public final CharSequence value;
    public final int start;
    public final int end;

    public Entity(CharSequence value, int start, int end) {
        this.value = value;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return value + "[" + start + "," + end + "]";
    }
}
