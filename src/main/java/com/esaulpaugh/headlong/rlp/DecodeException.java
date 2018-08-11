package com.esaulpaugh.headlong.rlp;

/**
 * Thrown to indicate a failed attempt to decode illegal or otherwise undecodeable RLP data.
 */
public class DecodeException extends Exception {

    public DecodeException(String msg) {
        super(msg);
    }

    public DecodeException(Throwable cause) {
        super(cause);
    }

}
