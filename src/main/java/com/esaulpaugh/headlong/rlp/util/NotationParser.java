package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.abi.beta.util.Pair;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class NotationParser {

    private static final int OBJECT_ARRAY = 0;
    private static final int STRING = 1;

    private static final int OBJECT_ARRAY_PREFIX_LEN = Notation.OBJECT_ARRAY_PREFIX.length();
    private static final int OBJECT_ARRAY_SUFFIX_LEN = Notation.OBJECT_ARRAY_SUFFIX.length();
    private static final int STRING_PREFIX_LEN = Notation.STRING_PREFIX.length();
    private static final int STRING_SUFFIX_LEN = Notation.STRING_SUFFIX.length();

    public static List<Object> parse(String notation) {
        List<Object> top = new ArrayList<>();
        parse(notation, 0, notation.length(), top);
        return top;
    }

    private static int parse(String notation, int i, final int end, List<Object> parent) {

        while (i < end) {

            int endArray = notation.indexOf(Notation.OBJECT_ARRAY_SUFFIX, i);
            if(endArray == -1) {
                endArray = Integer.MAX_VALUE;
            }
            Pair<Integer, Integer> nextObjectInfo = nextObject(notation, i);
            if(nextObjectInfo == null) {
                return Integer.MAX_VALUE;
            }

            final Integer nextObjectndex = nextObjectInfo.second;

            if(endArray < nextObjectndex) {
                return endArray + OBJECT_ARRAY_SUFFIX_LEN;
            }

            final int nextObjectType = nextObjectInfo.first;

            int objectStart;
            int objectEnd;
            switch (nextObjectType) {
            case STRING:
                objectStart = nextObjectndex + STRING_PREFIX_LEN;
                objectEnd = notation.indexOf(Notation.STRING_SUFFIX, objectStart);
                parent.add(Hex.decode(notation.substring(objectStart, objectEnd)));
                i = objectEnd + STRING_SUFFIX_LEN;
                break;
            case OBJECT_ARRAY:
                objectStart = nextObjectndex + OBJECT_ARRAY_PREFIX_LEN;
                List<Object> childList = new ArrayList<>();
                i = parse(notation, objectStart, end, childList);
                parent.add(childList);
                break;
            default: /* do nothing */
            }
        }

        return end + OBJECT_ARRAY_SUFFIX_LEN;
    }

    private static Pair<Integer, Integer> nextObject(String rlpon, int i) {
        int o = rlpon.indexOf(Notation.OBJECT_ARRAY_PREFIX, i);
        int s = rlpon.indexOf(Notation.STRING_PREFIX, i);

        if(s == -1) {
            if(o == -1) {
                return null;
            }
            return new Pair<>(OBJECT_ARRAY, o);
        }
        if(o == -1) {
            return new Pair<>(STRING, s);
        }

        return o < s
                ? new Pair<>(OBJECT_ARRAY, o)
                : new Pair<>(STRING, s);
    }
}
