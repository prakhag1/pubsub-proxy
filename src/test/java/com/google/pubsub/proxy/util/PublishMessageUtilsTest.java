/* Copyright 2019 Google Inc. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License. */

package com.google.pubsub.proxy.util;

import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.protobuf.Timestamp;

@RunWith(MockitoJUnitRunner.class)
public class PublishMessageUtilsTest {

    @Test(expected = Exception.class)
    public void WhenStringIsNotProperlyFormattedExceptionIsThrownToTheUser() throws Exception {
        PublishMessageUtils.getTimeStamp("random string");
    }

    @Test
    public void WhenStringIsProperlyFormattedTimeStampIsReturnedToTheUser() throws Exception {
        Timestamp timestamp = Timestamp.newBuilder().setNanos(1000).setSeconds(1498692500).build();
        Assert.assertEquals(timestamp, PublishMessageUtils.getTimeStamp("2017-06-28T23:28:20.000001Z"));
    }

    @Test(expected = Exception.class)
    public void WhenAttributesIsNotAValidHashMapThenGetAllAttributesExceptionIsThrownToTheUser() throws Exception {
        PublishMessageUtils.getAllAttributes("random string");
    }

    @Test
    public void WhenAttributesIsAValidHashMapThenGetAllAttributesReturnsHashMapToTheUser() throws Exception {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("key", "value");
        Assert.assertEquals(map, PublishMessageUtils.getAllAttributes(map));
    }

}