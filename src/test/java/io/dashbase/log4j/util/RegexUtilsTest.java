package io.dashbase.log4j.util;

import org.junit.Assert;
import org.junit.Test;

public class RegexUtilsTest {

    @Test
    public void getRegexForSimpleDateFormat(){
        String regex1= RegexUtils.getRegexForSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Assert.assertEquals("\\d{4}-\\d{2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}",regex1);
        String regex2=RegexUtils.getRegexForSimpleDateFormat("yy-MM-dd HH:mm:ssz");
        Assert.assertEquals("\\d{2}-\\d{2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}[a-zA-Z-+:0-9]*",regex2);
    }

}