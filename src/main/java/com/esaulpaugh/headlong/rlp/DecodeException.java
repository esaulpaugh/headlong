package com.esaulpaugh.headlong.rlp;

/**
 * Thrown to indicate a failed attempt to decode illegal or otherwise undecodeable data.
 */
public abstract class DecodeException extends Exception {

    DecodeException(String msg) {
        super(msg);
    }

    DecodeException(Throwable cause) {
        super(cause);
    }

    abstract boolean isRecoverable();

}
