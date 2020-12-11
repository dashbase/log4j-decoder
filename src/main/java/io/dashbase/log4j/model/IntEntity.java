package io.dashbase.log4j.model;

public class IntEntity {
    public final int value;
    public final int start;
    public final int end;

    public IntEntity(int value, int start, int end) {
        this.value = value;
        this.start = start;
        this.end = end;
    }

    public static IntEntity from(CharSequence text, int start, int end) {
        return new IntEntity(Integer.parseInt(text, start, end, 10), start, end);
    }
}
