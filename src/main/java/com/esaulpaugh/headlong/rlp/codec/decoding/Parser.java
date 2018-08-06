package com.esaulpaugh.headlong.rlp.codec.decoding;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation.OBJECT_ARRAY_PREFIX;
import static com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation.OBJECT_ARRAY_SUFFIX;
import static com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation.STRING_PREFIX;
import static com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation.STRING_SUFFIX;

public class Parser {

    private static final int OBJECT_ARRAY = 0;
    private static final int STRING = 1;

    private static final int OBJECT_ARRAY_PREFIX_LEN = OBJECT_ARRAY_PREFIX.length();
    private static final int OBJECT_ARRAY_SUFFIX_LEN = OBJECT_ARRAY_SUFFIX.length();
    private static final int STRING_PREFIX_LEN = STRING_PREFIX.length();
    private static final int STRING_SUFFIX_LEN = STRING_SUFFIX.length();

    public static List<Object> parse(String stringRep) {
        List<Object> top = new ArrayList<>();
        parse(stringRep, 0, stringRep.length(), top);
        return top;
    }

    private static int parse(String stringRep, int i, final int end, List<Object> parent) {

        while (i < end) {

            int endArray = stringRep.indexOf(OBJECT_ARRAY_SUFFIX, i);
            if(endArray == -1) {
                endArray = Integer.MAX_VALUE;
//                throw new DecodeException("no array end found: " + i);
            }
            Pair<Integer, Integer> nextPrefix = nextPrefix(stringRep, i);
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
                objectEnd = stringRep.indexOf(STRING_SUFFIX, objectStart);
                parent.add(Hex.decode(stringRep.substring(objectStart, objectEnd)));
                i = objectEnd + STRING_SUFFIX_LEN;
                break;
            case OBJECT_ARRAY:
                objectStart = nextPrefix.getRight() + OBJECT_ARRAY_PREFIX_LEN;
                List<Object> childList = new ArrayList<>();
                i = parse(stringRep, objectStart, end, childList);
                parent.add(childList);
                break;
            }
        }

        return end + OBJECT_ARRAY_SUFFIX_LEN;
    }

    private static Pair<Integer, Integer> nextPrefix(String rlpon, int i) {
        int o = rlpon.indexOf(OBJECT_ARRAY_PREFIX, i);
        int s = rlpon.indexOf(STRING_PREFIX, i);

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
