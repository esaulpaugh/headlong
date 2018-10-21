package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.util.Strings;

import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.headlong.util.Strings.HEX;

/**
 * Decodes RLP object notation as defined by the {@link Notation} class.
 */
public class NotationParser {

    private static final int OBJECT_ARRAY = 0;
    private static final int STRING = 1;

    private static final int OBJECT_ARRAY_PREFIX_LEN = Notation.OBJECT_ARRAY_PREFIX.length();
    private static final int OBJECT_ARRAY_SUFFIX_LEN = Notation.OBJECT_ARRAY_SUFFIX.length();
    private static final int STRING_PREFIX_LEN = Notation.STRING_PREFIX.length();
    private static final int STRING_SUFFIX_LEN = Notation.STRING_SUFFIX.length();

    /**
     * Returns the object hierarchy represented by the notation.
     * @param notation
     * @return
     */
    public static List<Object> parse(String notation) {
        List<Object> top = new ArrayList<>();
        int[] pair = new int[2];
        parse(notation, 0, notation.length(), top, pair);
        return top;
    }

    private static int parse(String notation, int i, final int end, List<Object> parent, int[] pair) {

        int nextArrayEnd;

        int nextObjectType;
        int nextObjectIndex;

        int objectStart;
        int objectEnd;
        while (i < end) {

            nextArrayEnd = notation.indexOf(Notation.OBJECT_ARRAY_SUFFIX, i);
            if(nextArrayEnd == -1) {
                nextArrayEnd = Integer.MAX_VALUE;
            }
            nextObjectStart(notation, i, pair);
            nextObjectType = pair[0];
            nextObjectIndex = pair[1];

            if(nextObjectType == -1 && nextObjectIndex == -1) {
                return Integer.MAX_VALUE;
            }

            if(nextArrayEnd < nextObjectIndex) {
                return nextArrayEnd + OBJECT_ARRAY_SUFFIX_LEN;
            }

            switch (nextObjectType) {
            case STRING:
                objectStart = nextObjectIndex + STRING_PREFIX_LEN;
                objectEnd = notation.indexOf(Notation.STRING_SUFFIX, objectStart);
                parent.add(Strings.decode(notation.substring(objectStart, objectEnd), HEX));
                i = objectEnd + STRING_SUFFIX_LEN;
                break;
            case OBJECT_ARRAY:
                objectStart = nextObjectIndex + OBJECT_ARRAY_PREFIX_LEN;
                List<Object> childList = new ArrayList<>();
                i = parse(notation, objectStart, end, childList, pair);
                parent.add(childList);
                break;
            default: /* do nothing */
            }
        }

        return end + OBJECT_ARRAY_SUFFIX_LEN;
    }

    private static void nextObjectStart(String notation, int i, int[] pair) { // Pair<Integer, Integer>
        int o = notation.indexOf(Notation.OBJECT_ARRAY_PREFIX, i);
        int s = notation.indexOf(Notation.STRING_PREFIX, i);

        if(s == -1) {
            if(o == -1) {
                pair[0] = -1;
                pair[1] = -1;
                return;
            }
            pair[0] = OBJECT_ARRAY;
            pair[1] = o;
            return;
        }
        if(o == -1) {
            pair[0] = STRING;
            pair[1] = s;
            return;
        }
        if(o < s) {
            pair[0] = OBJECT_ARRAY;
            pair[1] = o;
            return;
        }
        pair[0] = STRING;
        pair[1] = s;
//        return new Pair<>(STRING, s);
    }
}
