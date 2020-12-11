package io.dashbase.log4j.model;

import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ConversionPatternEl {
	public enum Type {
		DATE("d", "date"),
		LEVEL("p", "level"),
		LOGGER("c", "logger"),
		MAP("K", "map", "MAP"),
		LOCATION("l", "location"),
		THREAD("t", "tn", "thread", "threadName"),
		MESSAGE("m", "msg", "message"),
		EXCEPTION("ex", "exception", "throwable", "rEx", "rException", "rThrowable", "xEx", "xException", "xThrowable"),
		FILE("F", "file"),
		CLASS("C", "class"),
		METHOD("M", "method"),
		MARKER("marker", "markerSimpleName"),
		NANO_TS("N", "nano"),
		PID("pid", "processId"),
		RELATIVE_TS("r", "relative"),
		SEQ("sn", "sequenceNumber"),
		THREAD_ID("T", "tid", "threadId"),
		THREAD_PRIORITY("tp", "threadPriority"),
		UUID("u", "uuid"),
		FQCN("fqcn"),
		LINE("L", "line"),
		NDC("x", "NDC"),
		MDC("X", "MDC", "mdc");

		public final List<String> patternNames;

		Type(String... patternNames) {
			this.patternNames = Arrays.asList(patternNames);
		}
	}

	private static final Map<String, Type> PATTERN_NAME_TO_TYPE;

	static {
		PATTERN_NAME_TO_TYPE = new HashMap<>();
		Arrays.asList(Type.values()).forEach((type) -> {
			type.patternNames.forEach((name) ->{
				PATTERN_NAME_TO_TYPE.put(name, type);
			});
		});
	}

	private boolean followedByQuotedString;
	private int beginIndex;
	private int length;
	private int minWidth = -1;
	private int maxWidth = -1;
	private Type type;
	private String placeholderName;
	private String modifier;

	// only used for timestamp
	public DateTimeFormatter dateTimeFormatter;
	public Optional<DateTimeFormatter> lenientDateTimeFormatter;
	public boolean hasDate;
	public boolean useCache;

	public boolean isFollowedByQuotedString() {
		return followedByQuotedString;
	}

	public void setFollowedByQuotedString(boolean followedByQuotedString) {
		this.followedByQuotedString = followedByQuotedString;
	}

	public int getBeginIndex() {
		return beginIndex;
	}

	public void setBeginIndex(int beginIndex) {
		this.beginIndex = beginIndex;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getMinWidth() {
		return minWidth;
	}

	public void setMinWidth(int minWidth) {
		this.minWidth = minWidth;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	public String getPlaceholderName() {
		return placeholderName;
	}

	public void setPlaceholderName(String placeholderName) {
		this.placeholderName = placeholderName;
		this.type = PATTERN_NAME_TO_TYPE.get(placeholderName);
	}

	public Type getType() {
		return type;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	@Override
	public String toString() {
		return "ConversionRule [modifier=" + modifier + ", placeholderName=" + placeholderName + "]";
	}
}
