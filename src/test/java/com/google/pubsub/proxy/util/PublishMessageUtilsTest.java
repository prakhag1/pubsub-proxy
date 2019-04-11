package com.google.pubsub.proxy.util;

import com.google.protobuf.Timestamp;
import com.google.pubsub.proxy.exceptions.GenericAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class PublishMessageUtilsTest {

    @Test(expected = GenericAPIException.class)
    public void WhenStringIsNotProperlyFormattedAGenericAPIExceptionIsThrownToTheUser() throws GenericAPIException {
        PublishMessageUtils.getTimeStamp("random string");
    }

    @Test
    public void WhenStringIsProperlyFormattedTimeStampIsReturnedToTheUser() throws GenericAPIException {
        Timestamp timestamp = Timestamp.newBuilder().setNanos(1000).setSeconds(1498692500).build();
        Assert.assertEquals(timestamp, PublishMessageUtils.getTimeStamp("2017-06-28T23:28:20.000001Z"));
    }

    @Test(expected = GenericAPIException.class)
    public void WhenAttributesIsNotAValidHashMapThenGetAllAttributesAGenericAPIExceptionIsThrownToTheUser() throws GenericAPIException {
        PublishMessageUtils.getAllAttributes("random string");
    }

    @Test
    public void WhenAttributesIsAValidHashMapThenGetAllAttributesReturnsHashMapToTheUser() throws GenericAPIException {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("key", "value");
        Assert.assertEquals(map, PublishMessageUtils.getAllAttributes((Map)map));
    }

}