package com.esaulpaugh.headlong.abi.util;

public class ClassNames {

    public static String toFriendly(String className) {

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
            final int dot = base.lastIndexOf('.');
            if(dot != -1) {
                if (base.charAt(0) == 'L') {
                    sb.append(base.substring(dot + 1, base.length() - 1));
                } else {
                    sb.append(base.substring(dot + 1));
//                System.err.println("???");
//                return className;
                }
            }
        }
        }

        for (int i = 0; i < split; i++) {
            sb.append("[]");
        }

        return sb.toString();
    }

}
