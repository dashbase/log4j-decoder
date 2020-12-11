package io.dashbase.log4j.conversion;

import io.dashbase.log4j.model.ConversionPatternEl;
import io.dashbase.log4j.util.DateTimeFormatUtils;
import io.dashbase.log4j.util.RegexUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attribution:
 * Parts of this class was copied from:
 * https://github.com/logsaw/logsaw-app/blob/f72691ec91ce4559eb818f47b33bc2871f6ea95b/net.sf.logsaw.dialect.log4j/src/net/sf/logsaw/dialect/log4j/pattern/Log4JConversionPatternTranslator.java
 */
public class ConversionPatternParser {
    private static final Pattern EXTRACTION_PATTERN = Pattern.compile("%(-?(\\d+))?(\\.(\\d+))?([a-zA-Z]+)(\\{([^\\}]+)\\})*");

    private final ZoneId defaultTimeZone;

    public ConversionPatternParser(ZoneId defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
    }

    public Pattern getRegexPattern(String conversionPattern) {
        return Pattern.compile(toRegexPattern(conversionPattern));
    }

    private String toRegexPattern(String conversionPattern) {
        int idx = 0;
        List<ConversionPatternEl> els = extractConversionPattern(conversionPattern);
        ConversionPatternEl prevRule = null;
        for (ConversionPatternEl rule : els) {
            if ((rule.getBeginIndex() > idx) && (prevRule != null)) {
                prevRule.setFollowedByQuotedString(true);
            }
            idx = rule.getBeginIndex();
            idx += rule.getLength();
            prevRule = rule;
        }
        if ((conversionPattern.length() > idx) && (prevRule != null)) {
            prevRule.setFollowedByQuotedString(true);
        }
        StringBuilder sb = new StringBuilder();
        idx = 0;
        for (ConversionPatternEl el : els) {
            if (el.getBeginIndex() > idx) {
                sb.append(Pattern.quote(conversionPattern.substring(idx, el.getBeginIndex())));
            }
            idx = el.getBeginIndex();
            String regex = getRegexForPatternEl(el);
            sb.append(regex);
            idx += el.getLength();
        }
        if (conversionPattern.length() > idx) {
            sb.append(Pattern.quote(conversionPattern.substring(idx)));
        }
        return sb.toString();
    }

    /**
     * convert Log4j conversionPattern to conversion rule we use in parser
     */
    public List<ConversionPatternEl> extractConversionPattern(String conversionPattern) {
        Matcher m = EXTRACTION_PATTERN.matcher(conversionPattern);
        List<ConversionPatternEl> ret = new ArrayList<ConversionPatternEl>();
        while (m.find()) {
            String minWidthModifier = m.group(2);
            String maxWidthModifier = m.group(4);
            String conversionName = m.group(5);
            String conversionModifier = m.group(7);
            int minWidth = -1;
            if ((minWidthModifier != null) && (minWidthModifier.length() > 0)) {
                minWidth = Integer.parseInt(minWidthModifier);
            }
            int maxWidth = -1;
            if ((maxWidthModifier != null) && (maxWidthModifier.length() > 0)) {
                maxWidth = Integer.parseInt(maxWidthModifier);
            }
            ConversionPatternEl rule = new ConversionPatternEl();
            rule.setBeginIndex(m.start());
            rule.setLength(m.end() - m.start());
            rule.setMaxWidth(maxWidth);
            rule.setMinWidth(minWidth);
            rule.setPlaceholderName(conversionName);
            rule.setModifier(conversionModifier);
            rewrite(rule);
            ret.add(rule);
        }
        return ret;
    }

    /**
     * convert ConversionPatternEl to Regex
     */
    private String getRegexForPatternEl(ConversionPatternEl el) {
        switch (el.getPlaceholderName()) {
            case "d":
            case "date":
                return "(" + RegexUtils.getRegexForSimpleDateFormat(el.getModifier()) + ")";
            case "p":
            case "level":
                String lnHint = RegexUtils.getLengthHint(el);
                if (lnHint.length() > 0) {
                    return "([ A-Z]" + lnHint + ")";
                }
                return "([A-Z]{4,5})";
            case "c":
            case "logger":
            case "t":
            case "tn":
            case "thread":
            case "threadName":
            case "F":
            case "file":
            case "C":
            case "class":
            case "K":
            case "map":
            case "MAP":
            case "M":
            case "method":
            case "x":
            case "NDC":
            case "X":
            case "MDC":
            case "mdc":
            case "l":
            case "location":
            case "marker":
            case "markerSimpleName":
            case "u":
            case "uuid":
            case "fqcn":
                return "(.*" + RegexUtils.getLengthHint(el) + RegexUtils.getLazySuffix(el) + ")";
            // message
            case "m":
            case "msg":
            case "message":
            // stack trace
            case "ex":
            case "exception":
            case "throwable":
            case "rEx":
            case "rException":
            case "rThrowable":
            case "xEx":
            case "xException":
            case "xThrowable":
                return "((?s).*" + RegexUtils.getLengthHint(el) + RegexUtils.getLazySuffix(el) + ")";
            case "L":
            case "line":
            case "N":
            case "nano":
            case "pid":
            case "processId":
            case "r":
            case "relative":
            case "sn":
            case "sequenceNumber":
            case "T":
            case "tid":
            case "threadId":
            case "tp":
            case "threadPriority":
                return "([0-9]*" + RegexUtils.getLengthHint(el) + ")";
            // not supported yet
            /*
            case "enc":
            case "encode":
            case "equals":
            case "equalsIgnoreCase":
            case "highlight":
            case "maxLen|maxLength":
            case "n":
            case "variablesNotEmpty|varsNotEmpty|notEmpty":
            case "replace":
            case "style":
            case "endOfBatch":
             */
        }
        throw new IllegalArgumentException("cannot find the corresponding regex pattern for the placeholder: " + el.getPlaceholderName());
    }

    public String prepare(String conversionPattern) {
        if (!conversionPattern.endsWith("%n")) {
            return conversionPattern;
        }
        conversionPattern = conversionPattern.substring(0, conversionPattern.length() - 2);
        if (conversionPattern.contains("%n")) {
            throw new IllegalArgumentException("ConversionPattern is illegal!");
        }
        return conversionPattern;
    }

    private void rewrite(ConversionPatternEl el) {
        if (el.getPlaceholderName().equals("d")) {
            if (el.getModifier() == null) {
                // ISO8601 is the default
                el.setModifier("DEFAULT");
            }

            // obtained those patterns from log4j source code.
            // log4j-core/src/main/java/org/apache/logging/log4j/core/util/datetime/FixedDateFormat.java
            switch (el.getModifier()) {
                case "ABSOLUTE":
                    el.setModifier("HH:mm:ss,SSS");
                    break;
                case "ABSOLUTE_MICROS":
                    el.setModifier("HH:mm:ss,nnnnnn");
                    break;
                case "ABSOLUTE_NANOS":
                    el.setModifier("HH:mm:ss,nnnnnnnnn");
                    break;
                case "ABSOLUTE_PERIOD":
                    el.setModifier("HH:mm:ss.SSS");
                    break;
                case "COMPACT":
                    el.setModifier("yyyyMMddHHmmssSSS");
                    break;
                case "DATE":
                    el.setModifier("dd MMM yyyy HH:mm:ss,SSS");
                    break;
                case "DATE_PERIOD":
                    el.setModifier("dd MMM yyyy HH:mm:ss.SSS");
                    break;
                case "DEFAULT":
                    el.setModifier("yyyy-MM-dd HH:mm:ss,SSS");
                    break;
                case "DEFAULT_MICROS":
                    el.setModifier("yyyy-MM-dd HH:mm:ss,nnnnnn");
                    break;
                case "DEFAULT_NANOS":
                    el.setModifier("yyyy-MM-dd HH:mm:ss,nnnnnnnnn");
                    break;
                case "DEFAULT_PERIOD":
                    el.setModifier("yyyy-MM-dd HH:mm:ss.SSS");
                    break;
                case "ISO8601_BASIC":
                    el.setModifier("yyyyMMdd'T'HHmmss,SSS");
                    break;
                case "ISO8601_BASIC_PERIOD":
                    el.setModifier("yyyyMMdd'T'HHmmss.SSS");
                    break;
                case "ISO8601":
                    el.setModifier("yyyy-MM-dd'T'HH:mm:ss,SSS");
                    break;
                case "ISO8601_OFFSET_DATE_TIME_HH":
                    el.setModifier("yyyy-MM-dd'T'HH:mm:ss,SSSX");
                    break;
                case "ISO8601_OFFSET_DATE_TIME_HHMM":
                    el.setModifier("yyyy-MM-dd'T'HH:mm:ss,SSSXX");
                    break;
                case "ISO8601_OFFSET_DATE_TIME_HHCMM":
                    el.setModifier("yyyy-MM-dd'T'HH:mm:ss,SSSXXX");
                    break;
                case "ISO8601_PERIOD":
                    el.setModifier("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    break;

                default:
            }

            String format = el.getModifier();
            el.dateTimeFormatter = DateTimeFormatter.ofPattern(format);
            try {
                el.lenientDateTimeFormatter =
                    Optional.of(DateTimeFormatter.ofPattern(DateTimeFormatUtils.toLenientPattern(format)));
            } catch (IllegalArgumentException e) {
                // if lenient pattern is invalid
                el.lenientDateTimeFormatter = Optional.empty();
            }
            el.hasDate = DateTimeFormatUtils.containsDate(format);
            el.useCache = !DateTimeFormatUtils.containsMilliSecond(format);
            format = format.toLowerCase();
            if (el.dateTimeFormatter.getZone() == null && !(format.contains("x") || format.contains("z"))) {
                // if timestamp doesn't specify timezone, use defaultTimeZone instad.
                el.dateTimeFormatter = el.dateTimeFormatter.withZone(defaultTimeZone);
                el.lenientDateTimeFormatter.ifPresent(dtf -> dtf.withZone(defaultTimeZone));
            }
        }
    }
}