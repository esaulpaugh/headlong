package com.esaulpaugh.headlong.rlp.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private static final int OBJECT_ARRAY = 0;
    private static final int STRING = 1;

    private static final int OBJECT_ARRAY_PREFIX_LEN = ObjectNotation.OBJECT_ARRAY_PREFIX.length();
    private static final int OBJECT_ARRAY_SUFFIX_LEN = ObjectNotation.OBJECT_ARRAY_SUFFIX.length();
    private static final int STRING_PREFIX_LEN = ObjectNotation.STRING_PREFIX.length();
    private static final int STRING_SUFFIX_LEN = ObjectNotation.STRING_SUFFIX.length();

    public static List<Object> parse(String notation) {
        List<Object> top = new ArrayList<>();
        parse(notation, 0, notation.length(), top);
        return top;
    }

    private static int parse(String notation, int i, final int end, List<Object> parent) {

        while (i < end) {

            int endArray = notation.indexOf(ObjectNotation.OBJECT_ARRAY_SUFFIX, i);
            if(endArray == -1) {
                endArray = Integer.MAX_VALUE;
//                throw new DecodeException("no array end found: " + i);
            }
            Pair<Integer, Integer> nextPrefix = nextPrefix(notation, i);
            if(nextPrefix == null) {
                return Integer.MAX_VALUE;
            }
            if(endArray < nextPrefix.getRight()) {
                return endArray + OBJECT_ARRAY_SUFFIX_LEN;
            }

            int objectType = nextPrefix.getLeft();

            int objectStart;
            int objectEnd;
            switch (objectType) {
            case STRING:
                objectStart = nextPrefix.getRight() + STRING_PREFIX_LEN;
                objectEnd = notation.indexOf(ObjectNotation.STRING_SUFFIX, objectStart);
                parent.add(Hex.decode(notation.substring(objectStart, objectEnd)));
                i = objectEnd + STRING_SUFFIX_LEN;
                break;
            case OBJECT_ARRAY:
                objectStart = nextPrefix.getRight() + OBJECT_ARRAY_PREFIX_LEN;
                List<Object> childList = new ArrayList<>();
                i = parse(notation, objectStart, end, childList);
                parent.add(childList);
                break;
            default: /* do nothing */
            }
        }

        return end + OBJECT_ARRAY_SUFFIX_LEN;
    }

    private static Pair<Integer, Integer> nextPrefix(String rlpon, int i) {
        int o = rlpon.indexOf(ObjectNotation.OBJECT_ARRAY_PREFIX, i);
        int s = rlpon.indexOf(ObjectNotation.STRING_PREFIX, i);

        if(s == -1) {
            if(o == -1) {
                return null;
            }
            return new ImmutablePair<>(OBJECT_ARRAY, o);
        }
        if(o == -1) {
            return new ImmutablePair<>(STRING, s);
        }

        return o < s
                ? new ImmutablePair<>(OBJECT_ARRAY, o)
                : new ImmutablePair<>(STRING, s);
    }
}
