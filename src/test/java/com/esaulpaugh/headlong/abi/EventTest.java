package com.esaulpaugh.headlong.abi;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class EventTest {

    @Test
    public void testEvent() throws ParseException {
        final String name = "ahoy";
        final boolean[] indexed = new boolean[] { false, false, true, false, true };
        final String paramsString ="(int,uint,(),bool[],ufixed256x10)";
        Event event = new Event(name, paramsString, indexed);

        Assert.assertEquals(name, event.getName());
        Assert.assertEquals("(int256,uint256,(),bool[],ufixed256x10)", event.getParamsString());
        Assert.assertArrayEquals(indexed, event.getIndexManifest());
        Assert.assertFalse(event.isAnonymous());

        Assert.assertEquals(TupleType.parse("((),ufixed256x10)"), event.getIndexedParams());
        Assert.assertEquals(TupleType.parse("(int256,uint256,bool[])"), event.getNonIndexedParams());
    }

}
