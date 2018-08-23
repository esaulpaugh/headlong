package com.esaulpaugh.headlong.abi;

import java.text.ParseException;

public class NonTerminationException extends ParseException {

    /**
     * Constructs a ParseException with the specified detail message and
     * offset.
     * A detail message is a String that describes this particular exception.
     *
     * @param s           the detail message
     * @param errorOffset the position where the error is found while parsing.
     */
    public NonTerminationException(String s, int errorOffset) {
        super(s, errorOffset);
    }

//    public final int tupleStart;

//    /**
//     *
//     * @param s             the detail message
//     * @param tupleStart    the index of the non-terminating tuple
//     * @param errorOffset   the position where the error is found while parsing.
//     */
//    public NonTerminationException(String s, int tupleStart, int errorOffset) {
//        super(s, errorOffset);
//        this.tupleStart = tupleStart;
//    }

}
