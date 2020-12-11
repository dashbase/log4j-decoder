package io.dashbase.parser.log4j.conversion;

import io.dashbase.parser.log4j.model.ConversionPatternEl;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneOffset;
import java.util.List;

public class ConversionPatternParserTest {

    @Test
    public void extractConversionPattern(){
        String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m";
        List<ConversionPatternEl> els = new ConversionPatternParser(ZoneOffset.UTC).extractConversionPattern(pattern);
        Assert.assertEquals("d",els.get(0).getPlaceholderName());
        Assert.assertEquals("yyyy-MM-dd HH:mm:ss",els.get(0).getModifier());
        Assert.assertEquals("p",els.get(1).getPlaceholderName());
        Assert.assertEquals("c",els.get(2).getPlaceholderName());
        Assert.assertEquals("1",els.get(2).getModifier());
        Assert.assertEquals("L",els.get(3).getPlaceholderName());
        Assert.assertEquals("m",els.get(4).getPlaceholderName());
    }

    @Test
    public void prepare() {
        String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
        Assert.assertEquals("%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m", new ConversionPatternParser(ZoneOffset.UTC).prepare(pattern));
    }

}