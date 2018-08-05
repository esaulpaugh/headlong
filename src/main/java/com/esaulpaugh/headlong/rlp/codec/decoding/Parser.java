package com.esaulpaugh.headlong.rlp.codec.decoding;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;

import java.text.ParseException;
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

//    enum ObjectType {
//        OBJECT_ARRAY( OBJECT_ARRAY_PREFIX, OBJECT_ARRAY_SUFFIX ),
//        STRING( STRING_PREFIX, STRING_SUFFIX );
////        CHARACTER("\'", "\'");
//
//        private final String prefix;
//        private final int prefixLen;
//
//        private String suffix;
//        private int suffixLen;
//
//        ObjectType(String prefix, String suffix) {
//            this.prefix = prefix;
//            this.prefixLen = prefix.length();
//            this.suffix = suffix;
//            this.suffixLen = suffix.length();
//        }
//    }

    public static List<Object> parse(String stringRep) throws ParseException {
        List<Object> top = new ArrayList<>();
        parse(stringRep, 0, stringRep.length(), top);
        return top;
    }

    private static int parse(String stringRep, int i, final int end, List<Object> parent) throws ParseException {

        while (i < end) {

            int endArray = stringRep.indexOf(OBJECT_ARRAY_SUFFIX, i);
            if(endArray == -1) {
                throw new ParseException("no array end found", i);
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
//                String escaped = stringRep.substring(objectStart, objectEnd);
//                String unescaped = StringEscapeUtils.unescapeJava(escaped);
                parent.add(Hex.decode(stringRep.substring(objectStart, objectEnd)));
                i = objectEnd + STRING_SUFFIX_LEN;
                break;
//            case CHARACTER:
//                objectStart = i;
//                objectEnd = stringRep.indexOf(ObjectType.CHARACTER.suffix, objectStart);
//                escaped = stringRep.substring(objectStart, objectEnd);
//                unescaped = StringEscapeUtils.unescapeJava(escaped);
//                parent.add(unescaped.charAt(0));
//                i = objectEnd + ObjectType.CHARACTER.suffixLen;
//                break;
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

//    private static final Comparator<ImmutablePair<ObjectType, Integer>> COMPARE_BY_INT = Comparator.comparingInt(p -> p.right);
//
//    private static Pair<ObjectType, Integer> nextPrefix(String stringRep, int i) {
//
//        ArrayList<ImmutablePair<ObjectType, Integer>> indices = new ArrayList<>();
//        for (ObjectType type : ObjectType.values()) {
//            indices.add(new ImmutablePair<>(type, stringRep.indexOf(type.prefix, i)));
//        }
//
//        return indices.stream()
//                .filter(p -> p.right != -1)
//                .min(COMPARE_BY_INT)
//                .orElse(null);
//    }

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
