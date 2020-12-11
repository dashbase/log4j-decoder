package io.dashbase.log4j.parser;

import io.dashbase.log4j.model.Log4jLoggingEvent;
import org.junit.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class Log4jDecoderTest {
    @Test
    public void testNoYear() {
        String message = "09-26 23:08:06/UTC ERROR TestLog:49 - oops, error found";
        Log4jDecoder log4jDecoder = new Log4jDecoder("%d{MM-dd HH:mm:ss/zzz} %-5p %c{1}:%L - %m%n");
        Log4jLoggingEvent event = log4jDecoder.parseString(message);

        ZonedDateTime today = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(event.timestamp, ZoneOffset.UTC);

        assertEquals(today.getYear(), zdt.getYear());
        assertEquals(9, zdt.getMonthValue());
        assertEquals(26, zdt.getDayOfMonth());
        assertEquals(23, zdt.getHour());
        assertEquals(8, zdt.getMinute());
        assertEquals(6, zdt.getSecond());
        assertEquals(ZonedDateTime.parse(today.getYear() + "-09-26T23:08:06", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)).toInstant().toEpochMilli(),
                event.timestamp.toEpochMilli());
    }

    @Test
    public void parseString(){
        String message = "2017-09-26 23:08:06/UTC ERROR TestLog:49 - oops, error found";
        Log4jDecoder log4jDecoder = new Log4jDecoder("%d{yyyy-MM-dd HH:mm:ss/zzz} %-5p %c{1}:%L - %m%n");
        Log4jLoggingEvent event = log4jDecoder.parseString(message);
        assertEquals("oops, error found",event.message.value);
        assertEquals("oops, error found", message.substring(event.message.start, event.message.end));
        assertEquals("ERROR[24,29]",event.level.toString());
        assertEquals(1506467286000L, event.timestamp.toEpochMilli());
        assertEquals("TestLog",event.loggerName.value);
        assertEquals(49, event.locLine.value);
        assertEquals(38, event.locLine.start);
        assertEquals(40, event.locLine.end);

        // Try different timezone
        event = log4jDecoder.parseString("2017-09-26 23:08:06/PDT ERROR TestLog:49 - oops, error found");
        assertEquals(1506492486000L, event.timestamp.toEpochMilli());

        // No timezone. Assume UTC
        log4jDecoder = new Log4jDecoder("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
        event = log4jDecoder.parseString("2017-09-26 23:08:06 ERROR TestLog:49 - oops, error found");
        assertEquals(1506467286000L, event.timestamp.toEpochMilli());

        // No date, just timestamp
        log4jDecoder = new Log4jDecoder("%d{HH:mm:ss} %-5p %c{1}:%L - %m%n");
        event = log4jDecoder.parseString("23:08:06 ERROR TestLog:49 - oops, error found");
        ZonedDateTime zdt = ZonedDateTime.ofInstant(event.timestamp, ZoneOffset.UTC);
        ZonedDateTime today = ZonedDateTime.now(ZoneOffset.UTC);
        assertEquals(today.getYear(), zdt.getYear());
        assertEquals(today.getMonthValue(), zdt.getMonthValue());
        assertEquals(today.getDayOfMonth(), zdt.getDayOfMonth());
        assertEquals(23, zdt.getHour());
        assertEquals(8, zdt.getMinute());
        assertEquals(6, zdt.getSecond());

        // ABSOLUTE timestamp
        log4jDecoder = new Log4jDecoder("%d{ABSOLUTE} %-5p %c{1}:%L - %m%n");
        event = log4jDecoder.parseString("23:08:06,459 ERROR TestLog:49 - oops, error found");
        zdt = ZonedDateTime.ofInstant(event.timestamp, ZoneOffset.UTC);
        assertEquals(today.getYear(), zdt.getYear());
        assertEquals(today.getMonthValue(), zdt.getMonthValue());
        assertEquals(today.getDayOfMonth(), zdt.getDayOfMonth());
        assertEquals(23, zdt.getHour());
        assertEquals(8, zdt.getMinute());
        assertEquals(6, zdt.getSecond());

        // DEFAULT timestamp
        log4jDecoder = new Log4jDecoder("%d{DEFAULT} %-5p %c{1}:%L - %m%n");
        event = log4jDecoder.parseString("2017-09-26 23:08:06,000 ERROR TestLog:49 - oops, error found");
        assertEquals(1506467286000L, event.timestamp.toEpochMilli());

    }

    @Test
    public void parseTimestampWithPadModifier() throws Exception {

        String message = "2017- 9- 6 23:   8:06/UTC ERROR TestLog:49 - oops, error found";
        Log4jDecoder log4jDecoder = new Log4jDecoder("%d{yyyy-ppM-ppd HH:ppppm:ss/zzz} %-5p %c{1}:%L - %m%n");
        Log4jLoggingEvent event = log4jDecoder.parseString(message);
        assertEquals("2017-09-06T23:08:06Z", event.timestamp.toString());

        String dateStringWithSpace = "2019 Nov  4 04:28:21/UTC  ERROR TestLog:50 - oops, error1 found1";
        log4jDecoder = new Log4jDecoder("%d{yyyy MMM ppd HH:mm:ss/zzz} %-5p %c{1}:%L - %m%n");
        event = log4jDecoder.parseString(dateStringWithSpace);
        assertEquals("2019-11-04T04:28:21Z", event.timestamp.toString());
        String messageString = "2019 Nov 14 04:28:21/UTC  ERROR TestLog:51 - oops, error1 found1";
        event = log4jDecoder.parseString(messageString);
        assertEquals("2019-11-14T04:28:21Z", event.timestamp.toString());

    }


    @Test
    public void parseWithTimezone() throws Exception {
        String msg = "[2018-02-27 14:13:18,852] [thread1] INFO  stack.Message: mymsg";
        String format = "[%d] [%t] %-5p %c{2}: %m%n";

        ZoneId zid = ZoneId.of("America/Los_Angeles");
        Log4jDecoder log4jDecoder = new Log4jDecoder(format, zid);
        Log4jLoggingEvent event = log4jDecoder.parseString(msg);
        assertEquals("2018-02-27T22:13:18.852Z", event.timestamp.toString());
        assertEquals("INFO[36,40]", event.level.toString());
        assertEquals("thread1", event.thread.value);
        assertEquals("thread1", msg.substring(event.thread.start, event.thread.end));
        assertEquals("mymsg", event.message.value);


        zid = ZoneId.of("UTC");
        log4jDecoder = new Log4jDecoder(format, zid);
        event = log4jDecoder.parseString(msg);
        assertEquals("2018-02-27T14:13:18.852Z", event.timestamp.toString());

        zid = ZoneId.of("GMT");
        log4jDecoder = new Log4jDecoder(format, zid);
        event = log4jDecoder.parseString(msg);
        assertEquals("2018-02-27T14:13:18.852Z", event.timestamp.toString());
    }

    @Test
    public void parseSingleDigitTimestamp() throws Exception {
        String format = "%d{MMM dd, yyyy hh:mm:ss a zzz}";
        var decoder = new Log4jDecoder(format);

        String msg = "Jun 13, 2019 7:26:5 AM GMT";
        var event = decoder.parseString(msg);
        assertEquals("2019-06-13T07:26:05Z", event.timestamp.toString());
    }

    @Test
    public void testTimezone() {
        String dateString = "2018-02-28 12:00:00,000";
        String format = "%d";
        LocalDateTime dateTime =
                LocalDateTime.of(2018, 2, 28, 12, 0, 0, 0);

        Log4jDecoder log4jDecoder = new Log4jDecoder(format);
        Log4jLoggingEvent event = log4jDecoder.parseString(dateString);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.UTC).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());

        ZoneId tz = ZoneOffset.ofHours(8);
        log4jDecoder = new Log4jDecoder(format, tz);
        event = log4jDecoder.parseString(dateString);
        assertEquals(ZonedDateTime.of(dateTime, tz).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());

        // verify that Daylight Saving Time is handled properly
        String summerDateString = "2018-07-08 12:00:00,000";
        LocalDateTime summerDateTime =
                LocalDateTime.of(2018, 7, 8, 12, 0, 0, 0);

        tz = ZoneId.of("America/Los_Angeles");
        log4jDecoder = new Log4jDecoder(format, tz);
        event = log4jDecoder.parseString(dateString);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(-8)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());
        event = log4jDecoder.parseString(summerDateString);
        assertEquals(ZonedDateTime.of(summerDateTime, ZoneOffset.ofHours(-7)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());

        tz = ZoneId.of("PST", ZoneId.SHORT_IDS);
        log4jDecoder = new Log4jDecoder(format, tz);
        event = log4jDecoder.parseString(dateString);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(-8)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());
        event = log4jDecoder.parseString(summerDateString);
        assertEquals(ZonedDateTime.of(summerDateTime, ZoneOffset.ofHours(-7)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());

        String isoDateString = "2018-02-28T12:00:00,000";

        format = "%d{ISO8601}";
        log4jDecoder = new Log4jDecoder(format, ZoneOffset.ofHours(-7));
        event = log4jDecoder.parseString(isoDateString);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(-7)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());

        isoDateString = "2018-02-28T12:00:00,000";
        format = "%d{ISO8601}";
        log4jDecoder = new Log4jDecoder(format, ZoneId.of("JST", ZoneId.SHORT_IDS));
        event = log4jDecoder.parseString(isoDateString);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(9)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());
}

    @Test
    public void testTimezoneWithDefinedZone() {
        LocalDateTime dateTime =
                LocalDateTime.of(2018, 2, 28, 12, 0, 0, 0);
        // if timezone is provided in the timestamp, honor it.
        String dateStringWithTZ = "2018-02-28T12:00:00.000-0700";
        String format = "%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}";
        Log4jDecoder log4jDecoder = new Log4jDecoder(format);
        Log4jLoggingEvent event = log4jDecoder.parseString(dateStringWithTZ);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(-7)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());

        dateStringWithTZ = "2018-02-28 12:00:00.000 JST";
        format = "%d{yyyy-MM-dd HH:mm:ss.SSS z}";
        log4jDecoder = new Log4jDecoder(format);
        event = log4jDecoder.parseString(dateStringWithTZ);
        assertEquals(ZonedDateTime.of(dateTime, ZoneOffset.ofHours(9)).toInstant().toEpochMilli(), event.timestamp.toEpochMilli());
    }

    @Test
    public void testMdc() {
        String pattern = "[%d] [%t] %-5p %c{2}: %m - tx.id=%X{tx.id} tx.seg=%X{tx.segment} tx.p-seg=%X{tx.p-segment}%n";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);

        String text = "[2018-03-22 11:41:38,380] [WT2815 processing(213576)] INFO  server.control: none of the slaves picked up send disconnected event connAddr:Addr[595] msRedirectedAddr:Addr[3569] subScriptionAddrs:Addr[3569] - tx.id=1b7d2011-a108-4861-8b84-2cf3ca6b8503 tx.seg=7666714 tx.p-seg=7666709";
        Log4jLoggingEvent event = log4jDecoder.parseString(text);
        assertEquals("1b7d2011-a108-4861-8b84-2cf3ca6b8503[213,249]", event.mdc.get("tx.id").toString());
        assertEquals("1b7d2011-a108-4861-8b84-2cf3ca6b8503", text.substring(213, 249));
        assertEquals("7666714[257,264]", event.mdc.get("tx.segment").toString());
        assertEquals("7666714", text.substring(257, 264));
        assertEquals("7666709[274,281]", event.mdc.get("tx.p-segment").toString());
        assertEquals("7666709", text.substring(274, 281));

        text = "[2018-03-22 11:41:38,380] [WT2815 processing(213576)] INFO  server.control: gotReasonCode=null for call Call(Addr[591] => Addr[3598], CId[1521609755093]) - tx.id=1b7d2011-a108-4861-8b84-2cf3ca6b8503 tx.seg=7666714 tx.p-seg=7666709";
        event = log4jDecoder.parseString(text);
        assertEquals("1b7d2011-a108-4861-8b84-2cf3ca6b8503[162,198]", event.mdc.get("tx.id").toString());
        assertEquals("1b7d2011-a108-4861-8b84-2cf3ca6b8503", text.substring(162, 198));
        assertEquals("7666714[206,213]", event.mdc.get("tx.segment").toString());
        assertEquals("7666714", text.substring(206, 213));
        assertEquals("7666709[223,230]", event.mdc.get("tx.p-segment").toString());
        assertEquals("7666709", text.substring(223, 230));

        text = "[2018-03-22 11:41:38,380] [WT2815 processing(213576)] INFO  server.control: getConnectedSomewhere=false from getAttribute of setReasonCode Call(Addr[591] => Addr[3598], CId[1521609755093]) - tx.id=1b7d2011-a108-4861-8b84-2cf3ca6b8503 tx.seg=7666714 tx.p-seg=7666709";
        event = log4jDecoder.parseString(text);
        assertEquals("1b7d2011-a108-4861-8b84-2cf3ca6b8503[197,233]", event.mdc.get("tx.id").toString());
        assertEquals("7666714[241,248]", event.mdc.get("tx.segment").toString());
        assertEquals("7666709[258,265]", event.mdc.get("tx.p-segment").toString());

        pattern = "%X{test, test2}";
        log4jDecoder = new Log4jDecoder(pattern);
        text = "{test=123, test2=456}";
        event = log4jDecoder.parseString(text);
        assertEquals("123[6,9]", event.mdc.get("test").toString());
        assertEquals("123", text.substring(6, 9));
        assertEquals("456[17,20]", event.mdc.get("test2").toString());
        assertEquals("456", text.substring(17, 20));

        pattern = "%MDC";
        log4jDecoder = new Log4jDecoder(pattern);
        event = log4jDecoder.parseString("{test=123, test2=456}");
        assertEquals("123[6,9]", event.mdc.get("test").toString());
        assertEquals("123", text.substring(6, 9));
        assertEquals("456[17,20]", event.mdc.get("test2").toString());
        assertEquals("456", text.substring(17, 20));

        pattern = "%X{test}";
        log4jDecoder = new Log4jDecoder(pattern);
        event = log4jDecoder.parseString("   ");
        assertNull(event.mdc);

        pattern = "test=%X{test}";
        log4jDecoder = new Log4jDecoder(pattern);
        event = log4jDecoder.parseString("test=   ");
        assertNull(event.mdc);
    }

    @Test
    public void testNDC() throws  Exception {
        String pattern = "%x";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString("[test, test 1 2 3]");
        assertEquals("test, test 1 2 3", event.ndc.value);
    }

    @Test
    public void testException() {
        String pattern = "%ex";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        String trace =  "java.lang.IllegalArgumentException: test\n\tat Test.main(Test.java:8)";
        Log4jLoggingEvent event = log4jDecoder.parseString(trace);
        assertEquals(trace, event.throwableTrace.value);
        assertEquals(0, event.throwableTrace.start);
        assertEquals(trace.length(), event.throwableTrace.end);

        pattern = "%m - %ex";
        log4jDecoder = new Log4jDecoder(pattern);
        event = log4jDecoder.parseString("test message - " + trace);
        assertEquals("test message", event.message.value);
        assertEquals(trace, event.throwableTrace.value);

        pattern = "%ex{none}{filters(test)}{suffix(pattern)}";
        log4jDecoder = new Log4jDecoder(pattern);
        event = log4jDecoder.parseString(trace);
        assertEquals(trace, event.throwableTrace.value);
    }

    @Test
    public void testMarker() {
        String pattern = "%marker";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString("test_marker");
        assertEquals("test_marker", event.marker.value);
    }

    @Test
    public void testNano() {
        Instant now = Instant.now();
        String pattern = "%N";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString(String.valueOf(now.toEpochMilli() * 1000));
        assertEquals(now.toEpochMilli(), event.timestamp.toEpochMilli());
    }

    @Test
    public void testProcessId() {
        String pattern = "%pid";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString("12345");
        assertEquals(12345L, event.processId.value);
    }

    @Test
    public void testRelative() {
        String pattern = "%relative";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString("12345");
        assertEquals(12345L, event.relativeTimestamp.value);
    }

    @Test
    public void testThreadIdAndPriority() {
        String pattern = "%T %tp";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString("12345 5");
        assertEquals(12345L, event.threadId.value);
        assertEquals(5, event.threadPriority.value);
    }

    @Test
    public void testLocation() {
        String message = "2018-03-23 11:48:21,614 Test.main(Test.java:8) test";
        String pattern = "%d %l %m%n";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString(message);
        assertEquals("Test.main(Test.java:8)", event.location.value);
        assertEquals("Test.main(Test.java:8)", message.substring(event.location.start, event.location.end));
    }

    @Test
    public void testOtherPatter() {
        String pattern = "%d [%-6p] %C{1}.%M(%F:%L) - %m%n";
        Log4jDecoder log4jDecoder = new Log4jDecoder(pattern);
        Log4jLoggingEvent event = log4jDecoder.parseString("2016-06-20 19:25:42,249 [DEBUG ] Log4j2HelloWorldExample.methodOne(Log4j2HelloWorldExample.java:14) - Debug Message Logged !!");
        assertEquals("DEBUG[25,30]", event.level.toString());
        assertEquals("Log4j2HelloWorldExample", event.locClass.value);
        assertEquals("methodOne", event.locMethod.value);
        assertEquals("Log4j2HelloWorldExample.java", event.locFileName.value);
        assertEquals(14L, event.locLine.value);
        assertEquals("Debug Message Logged !!", event.message.value);
    }
}