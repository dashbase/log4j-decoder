package io.dashbase.parser.log4j.parser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.dashbase.parser.log4j.conversion.ConversionPatternParser;
import io.dashbase.parser.log4j.model.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Log4jDecoder {
    private final Cache<LocalDate, DateTimeFormatter> dateTimeFormatterCache =
        CacheBuilder.newBuilder().maximumSize(10).build();

    private final Cache<CharSequence, Instant> timestampCache =
        CacheBuilder.newBuilder().maximumSize(1000).build();

    private final List<ConversionPatternEl> extractedRules;
    private final Pattern pattern;
    private final ZoneId defaultTimeZone;


    public Log4jDecoder(String conversionPattern) {
        this(conversionPattern, ZoneOffset.UTC);
    }

    public Log4jDecoder(String conversionPattern, ZoneId defaultTimeZone) {
        ConversionPatternParser conversionPatternParser = new ConversionPatternParser(defaultTimeZone);
        conversionPattern = conversionPatternParser.prepare(conversionPattern);
        extractedRules = conversionPatternParser.extractConversionPattern(conversionPattern);
        pattern = conversionPatternParser.getRegexPattern(conversionPattern);
        this.defaultTimeZone = defaultTimeZone;
    }

    public Log4jLoggingEvent parseString(CharSequence line) {
        Matcher m = pattern.matcher(line);
        if (!m.matches()) {
            return null;
        }
        Log4jLoggingEvent currentEntry = new Log4jLoggingEvent();
        for (int i = 0; i < m.groupCount(); i++) {
            int start = m.start(i + 1);
            int end = m.end(i + 1);
            if (start < 0) continue;
            while (start < line.length() && line.charAt(start) == ' ') start++;
            while (end > 0 && line.charAt(end - 1) == ' ') end--;
            if (start < end) {
                extractField(currentEntry, line, start, end, extractedRules.get(i));
            }
        }
        return currentEntry;
    }

    private void extractField(Log4jLoggingEvent entry, CharSequence text, int start, int end, ConversionPatternEl rule) {
        switch (rule.getType()) {
            case DATE:
                if (rule.useCache) {
                    var timestamp = timestampCache.getIfPresent(text.subSequence(start, end));
                    if (timestamp != null) {
                        entry.timestamp = timestamp;
                        return;
                    }
                }

                // if the timestamp is in the log message, use it as the event timestamp
                DateTimeFormatter dtf = rule.dateTimeFormatter;
                // If the date pattern only contains time, use the today's year/month/day when parsing the input string.
                if (!rule.hasDate) {
                    LocalDate today = LocalDate.now(defaultTimeZone);
                    try {
                        dtf = dateTimeFormatterCache.get(today, () ->
                            new DateTimeFormatterBuilder().append(rule.dateTimeFormatter)
                                .parseDefaulting(ChronoField.YEAR, today.getYear())
                                .parseDefaulting(ChronoField.MONTH_OF_YEAR, today.getMonthValue())
                                .parseDefaulting(ChronoField.DAY_OF_MONTH, today.getDayOfMonth())
                                .toFormatter().withZone(defaultTimeZone));
                    } catch (ExecutionException e) {
                        throw new IllegalArgumentException(e);
                    }
                }

                ZonedDateTime zdt;
                try {
                    zdt = ZonedDateTime.parse(text.subSequence(start, end), dtf);
                } catch (DateTimeParseException e) {
                    if (rule.lenientDateTimeFormatter.isPresent()) {
                        zdt = ZonedDateTime.parse(text.subSequence(start, end), rule.lenientDateTimeFormatter.get());
                    } else {
                        throw e;
                    }
                }
                entry.timestamp = zdt.toInstant();
                if (rule.useCache) {
                    timestampCache.put(text.subSequence(start, end), entry.timestamp);
                }
                break;
            case LEVEL:
                entry.level = new Entity(text.subSequence(start, end), start, end);
                break;
            case LOGGER:
                entry.loggerName = new Entity(text.subSequence(start, end), start, end);
                break;
            case MAP:
                entry.map = parseToEntityMap(text, start, end);
                break;
            case LOCATION:
                entry.location = new Entity(text.subSequence(start, end), start, end);
                break;
            case THREAD:
                entry.thread = new Entity(text.subSequence(start, end), start, end);
                break;
            case MESSAGE:
                entry.message = new Entity(text.subSequence(start, end), start, end);
                break;
            case EXCEPTION:
                entry.throwableTrace = new Entity(text.subSequence(start, end), start, end);
                break;
            case FILE:
                entry.locFileName = new Entity(text.subSequence(start, end), start, end);
                break;
            case CLASS:
                entry.locClass = new Entity(text.subSequence(start, end), start, end);
                break;
            case METHOD:
                entry.locMethod = new Entity(text.subSequence(start, end), start, end);
                break;
            case MARKER:
                entry.marker = new Entity(text.subSequence(start, end), start, end);
                break;
            case NANO_TS:
                entry.timestamp = Instant.ofEpochMilli(Long.parseLong(text, start, end, 10) / 1000);
                break;
            case PID:
                entry.processId = LongEntity.from(text, start, end);
                break;
            case RELATIVE_TS:
                entry.relativeTimestamp = LongEntity.from(text, start, end);
                break;
            case SEQ:
                entry.sequenceNumber = LongEntity.from(text, start, end);
                break;
            case THREAD_ID:
                entry.threadId = LongEntity.from(text, start, end);
                break;
            case THREAD_PRIORITY:
                entry.threadPriority = IntEntity.from(text, start, end);
                break;
            case UUID:
                entry.uuid = new Entity(text.subSequence(start, end), start, end);
                break;
            case FQCN:
                entry.fqcn = new Entity(text.subSequence(start, end), start, end);
                break;
            case LINE:
                entry.locLine = LongEntity.from(text, start, end);
                break;
            case NDC:
                int bracketOffset = text.charAt(start) == '[' && text.charAt(end - 1) == ']' ? 1 : 0;
                entry.ndc = new Entity(text.subSequence(start + bracketOffset, end - bracketOffset),
                    start + bracketOffset, end - bracketOffset);
                break;
            case MDC:
                if (text.charAt(start) == '{' && text.charAt(end - 1) == '}') {
                    entry.putMdc(parseToEntityMap(text, start + 1, end - 1));
                } else {
                    entry.putMdc(rule.getModifier(), new Entity(text.subSequence(start, end), start, end));
                }
                break;
            default:
                throw new IllegalArgumentException("Cannot find the corresponding regex pattern :" + rule.getPlaceholderName());
        }
    }

    private Map<String, Entity> parseToEntityMap(CharSequence text, int startIndex, int endIndex) {
        var result = new HashMap<String, Entity>();

        int index = startIndex;
        while (index < endIndex) {
            // skip leading space
            while (text.charAt(index) == ' ') index++;
            // get key
            int keyStart = index;
            while (text.charAt(index) != '=') index++;
            String key = text.subSequence(keyStart, index).toString();
            index++;
            int valueStart = index;
            while (index < endIndex && text.charAt(index) != ',') index++;
            Entity value = new Entity(text.subSequence(valueStart, index), valueStart, index);
            result.put(key, value);
            index++;
        }

        return result;
    }

    @Override
    public String toString() {
        return "Log4jDecoder: " + pattern.toString();
    }
}
