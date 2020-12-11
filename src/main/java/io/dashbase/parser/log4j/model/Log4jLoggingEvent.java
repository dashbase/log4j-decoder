package io.dashbase.parser.log4j.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Log4jLoggingEvent {
    public Instant timestamp;

    // fully qualified class name of the logger
    public Entity fqcn;

    public Entity level;

    public Entity location;

    public Entity locClass;

    public Entity locFileName;

    public LongEntity locLine;

    public Entity locMethod;

    public Entity loggerName;

    public Map<String, Entity> map;

    public Entity marker;

    public Map<String, Entity> mdc;

    public Entity message;

    public Entity ndc;

    public LongEntity processId;

    public LongEntity relativeTimestamp;

    public LongEntity sequenceNumber;

    public Entity thread;

    public LongEntity threadId;

    public IntEntity threadPriority;

    public Entity throwableTrace;

    public Entity uuid;

    public void putMdc(String key, Entity value) {
        if(mdc == null) {
            mdc = new HashMap<>();
        }
        this.mdc.put(key, value);
    }

    public void putMdc(Map<String, Entity> mdc) {
        if(this.mdc == null) {
          this.mdc = new HashMap<>();
        }
        this.mdc.putAll(mdc);
    }
}
