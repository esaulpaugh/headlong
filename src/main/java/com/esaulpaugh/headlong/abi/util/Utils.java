package com.esaulpaugh.headlong.abi.util;

import java.nio.ByteBuffer;

public class Utils {

    public static int convertPos(ByteBuffer bb) {
        return (bb.position() - 4) >>> 5;
    }

    public static int convert(int pos) {
        return convertOffset(pos - 4);
    }

    public static int convertOffset(int pos) {
        return pos >>> 5;
    }

    public static int roundUp(int len) {
        int mod = len & 31;
        return mod == 0
                ? len
                : len + (32 - mod);
    }

    public static String getNameStub(Class<?> arrayClass) {
        if(arrayClass.isArray()) {
            String className = arrayClass.getName();
            if(className.charAt(0) == '[') {
                return className.substring(1);
            }
        }
        throw new IllegalArgumentException("unexpected class: " + arrayClass.getName());
    }

}
