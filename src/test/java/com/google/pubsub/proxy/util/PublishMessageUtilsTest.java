package com.google.pubsub.proxy.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.protobuf.Timestamp;

@RunWith(MockitoJUnitRunner.class)
public class PublishMessageUtilsTest {

    @Test(expected = Exception.class)
    public void WhenStringIsNotProperlyFormattedAGenericAPIExceptionIsThrownToTheUser() throws Exception {
        PublishMessageUtils.getTimeStamp("random string");
    }

    @Test
    public void WhenStringIsProperlyFormattedTimeStampIsReturnedToTheUser() throws Exception {
        Timestamp timestamp = Timestamp.newBuilder().setNanos(1000).setSeconds(1498692500).build();
        Assert.assertEquals(timestamp, PublishMessageUtils.getTimeStamp("2017-06-28T23:28:20.000001Z"));
    }

    @Test(expected = Exception.class)
    public void WhenAttributesIsNotAValidHashMapThenGetAllAttributesAGenericAPIExceptionIsThrownToTheUser() throws Exception {
        PublishMessageUtils.getAllAttributes("random string");
    }

    @Test
    public void WhenAttributesIsAValidHashMapThenGetAllAttributesReturnsHashMapToTheUser() throws Exception {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("key", "value");
        Assert.assertEquals(map, PublishMessageUtils.getAllAttributes((Map)map));
    }

}