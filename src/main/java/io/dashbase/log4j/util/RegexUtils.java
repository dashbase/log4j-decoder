package io.dashbase.log4j.util;

import io.dashbase.log4j.model.ConversionPatternEl;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Attribution:
 * Part of this class was copied from: 
 * https://github.com/logsaw/logsaw-app/blob/master/net.sf.logsaw.dialect.pattern/src/net/sf/logsaw/dialect/pattern/RegexUtils.java
 */
public class RegexUtils {
    private static final Pattern UNQUOTE_P_1 = Pattern.compile("'[^']+'");
    private static final Pattern UNQUOTE_P_2 = Pattern.compile("''");

    /**
     * Returns the Regex lazy suffix for the given rule.
     *
     * @param rule the conversion rule
     * @return the Regex lazy suffix
     */
    public static String getLazySuffix(ConversionPatternEl rule) {
        return rule.isFollowedByQuotedString() ? "?" : "";
    }

    /**
     * Returns the Regex length hint for the given rule.
     *
     * @param rule the conversion rule
     * @return the Regex length hint
     */
    public static String getLengthHint(ConversionPatternEl rule) {
        if ((rule.getMaxWidth() > 0) && (rule.getMaxWidth() == rule.getMinWidth())) {
            // Exact length specified
            return "{" + rule.getMaxWidth() + "}";
        } else if (rule.getMaxWidth() > 0) {
            // Both min and max are specified
            return "{" + Math.max(0, rule.getMinWidth()) + "," + rule.getMaxWidth() + "}"; //$NON-NLS-3$
        } else if (rule.getMinWidth() > 0) {
            // Only min is specified
            return "{" + rule.getMinWidth() + ",}";
        }
        return "";
    }

    public static String getRegexForSimpleDateFormat(String format) {

        // Initialize
        ReplacementContext ctx = new ReplacementContext();
        ctx.setBits(new BitSet(format.length()));
        ctx.setSb(new StringBuilder(format));

        // Unquote
        unquote(ctx);
        //pad modifier on the left with spaces to a width of 3 or more, Matches between 3 and 50 times
        ctx.replace("[p]{3,50}", "\\s+");
        //pad modifier on the left with spaces to a width of 1 or 2
        ctx.replace("[p]{1,2}", "\\s?");

        // G - Era designator
        ctx.replace("G+", "[ADBC]{2}");
        // y - Year
        ctx.replace("[y]{3,}", "\\d{4}");
        ctx.replace("[y]{2}", "\\d{2}");
        ctx.replace("y", "\\d{4}");
        // M - Month in year
        ctx.replace("[M]{3,}", "[a-zA-Z]*");
        ctx.replace("[M]{2}", "\\d{2}");
        ctx.replace("M", "\\d{1,2}");
        // w - Week in year
        ctx.replace("w+", "\\d{1,2}");
        // W - Week in month
        ctx.replace("W+", "\\d");
        // D - Day in year
        ctx.replace("D+", "\\d{1,3}");
        // d - Day in month
        ctx.replace("d+", "\\d{1,2}");
        // F - Day of week in month
        ctx.replace("F+", "\\d");
        // E - Day in week
        ctx.replace("E+", "[a-zA-Z]*");
        // a - Am/pm marker
        ctx.replace("a+", "[AMPM]{2}");
        // H - Hour in day (0-23)
        ctx.replace("H+", "\\d{1,2}");
        // k - Hour in day (1-24)
        ctx.replace("k+", "\\d{1,2}");
        // K - Hour in am/pm (0-11)
        ctx.replace("K+", "\\d{1,2}");
        // h - Hour in am/pm (1-12)
        ctx.replace("h+", "\\d{1,2}");
        // m - Minute in hour
        ctx.replace("m+", "\\d{1,2}");
        // s - Second in minute
        ctx.replace("s+", "\\d{1,2}");
        // S - fraction of second
        ctx.replace("S+", "\\d{1,6}");
        // n - nano second
        ctx.replace("n+", "\\d{1,9}");
        // S - Millisecond
        ctx.replace("S+", "\\d{1,6}");
        // V - Time zone ID
        ctx.replace("V+", "[a-zA-Z+-0-9_/]+");
        // z - Time zone
        ctx.replace("z+", "[a-zA-Z-+:0-9]*");
        // Z - Time zone
        ctx.replace("Z+", "[-+]\\d{4}");
        return ctx.getSb().toString();
    }

    private static void unquote(ReplacementContext ctx) {
        Matcher m = UNQUOTE_P_1.matcher(ctx.getSb().toString());
        while (m.find()) {
            // Match is valid
            int offset = -2;
            // Copy all bits after the match
            for (int i = m.end(); i < ctx.getSb().length(); i++) {
                ctx.getBits().set(i + offset, ctx.getBits().get(i));
            }
            for (int i = m.start(); i < m.end() + offset; i++) {
                ctx.getBits().set(i);
            }
            ctx.getSb().replace(m.start(), m.start() + 1, "");
            ctx.getSb().replace(m.end() - 2, m.end() - 1, "");
        }
        m = UNQUOTE_P_2.matcher(ctx.getSb().toString());
        while (m.find()) {
            // Match is valid
            int offset = -1;
            // Copy all bits after the match
            for (int i = m.end(); i < ctx.getSb().length(); i++) {
                ctx.getBits().set(i + offset, ctx.getBits().get(i));
            }
            for (int i = m.start(); i < m.end() + offset; i++) {
                ctx.getBits().set(i);
            }
            ctx.getSb().replace(m.start(), m.start() + 1, "");
        }
    }

    private static class ReplacementContext {

        private BitSet bits;
        private StringBuilder sb;

        public BitSet getBits() {
            return bits;
        }

        public void setBits(BitSet bits) {
            this.bits = bits;
        }

        public StringBuilder getSb() {
            return sb;
        }

        public void setSb(StringBuilder sb) {
            this.sb = sb;
        }

        public void replace(String regex, String replacement) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(getSb());
            while (m.find()) {
                int idx = getBits().nextSetBit(m.start());
                if ((idx == -1) || (idx > m.end() - 1)) {
                    // Match is valid
                    int len = m.end() - m.start();
                    int offset = replacement.length() - len;
                    if (offset > 0) {
                        // Copy all bits after the match, in reverse order
                        for (int i = getSb().length() - 1; i > m.end(); i--) {
                            getBits().set(i + offset, getBits().get(i));
                        }
                    } else if (offset < 0) {
                        // Copy all bits after the match
                        for (int i = m.end(); i < getSb().length(); i++) {
                            getBits().set(i + offset, getBits().get(i));
                        }
                    }
                    for (int i = m.start(); i < m.end() + offset; i++) {
                        getBits().set(i);
                    }
                    getSb().replace(m.start(), m.end(), replacement);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ReplacementContext [bits=");
            for (int i = 0; i < this.sb.length(); i++) {
                sb.append(bits.get(i) ? '1' : '0');
            }
            sb.append(", sb=");
            sb.append(this.sb);
            sb.append(']');
            return sb.toString();
        }
    }
}
