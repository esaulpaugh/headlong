package com.esaulpaugh.headlong.abi.util;

public class ClassNames {

    public static String toFriendly(String className) {
        return toFriendly(className, null);
    }

    public static String toFriendly(String className, Integer arrayLength) {

        StringBuilder sb = new StringBuilder();
        final int split = className.lastIndexOf('[') + 1;

        final String base = split > 0 ? className.substring(split) : className;
        switch (base) {
        case "B": sb.append("byte"); break;
        case "S": sb.append("short"); break;
        case "I": sb.append("int"); break;
        case "J": sb.append("long"); break;
        case "F": sb.append("float"); break;
        case "D": sb.append("double"); break;
        case "C": sb.append("char"); break;
        case "Z": sb.append("boolean"); break;
        default: {
            final int lastDotIndex = base.lastIndexOf('.');
            if(lastDotIndex != -1) {
                if (base.charAt(0) == 'L') {
                    sb.append(base, lastDotIndex + 1, base.length() - 1); // last char is semicolon
                } else {
                    sb.append(base, lastDotIndex + 1, base.length()); // i.e. base.substring(dot + 1)
                }
            }
        }
        }

        if(split > 0) {
            int i = 0;
            if(arrayLength != null && arrayLength >= 0) {
                sb.append('[').append(arrayLength).append(']');
                i++;
            }
            for ( ; i < split; i++) {
                sb.append("[]");
            }
        }

        return sb.toString();
    }

    public static String getArrayClassNameStub(Class<?> arrayClass) {
        if(arrayClass.isArray()) {
            String className = arrayClass.getName();
            if(className.charAt(0) == '[') {
                return className.substring(1);
            }
        }
        throw new IllegalArgumentException("unexpected class: " + arrayClass.getName());
    }
}
