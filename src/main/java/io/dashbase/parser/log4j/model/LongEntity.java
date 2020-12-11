package io.dashbase.parser.log4j.model;

public class LongEntity {
    public final long value;
    public final int start;
    public final int end;

    public LongEntity(long value, int start, int end) {
        this.value = value;
        this.start = start;
        this.end = end;
    }

    public static LongEntity from(CharSequence text, int start, int end) {
        return new LongEntity(Long.parseLong(text, start, end, 10), start, end);
    }
}
