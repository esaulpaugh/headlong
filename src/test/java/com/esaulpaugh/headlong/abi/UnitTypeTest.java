package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.jupiter.api.Test;

public class UnitTypeTest {

    @Test
    public void testConstructorAccess() throws Throwable {
        // should print to System.err:
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.UnitType
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.UnitType
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.UnitType
        TestUtils.assertThrown(IllegalStateException.class, "instance not permitted", () -> new IntType("custom", 300, true));
        TestUtils.assertThrown(IllegalStateException.class, "instance not permitted", () -> new LongType("custom", 300, true));
        TestUtils.assertThrown(IllegalStateException.class, "instance not permitted", () -> new BigIntegerType("custom", 300, true));
        System.out.println("Constraints checked successfully.");
    }

}
