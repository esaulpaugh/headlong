package com.esaulpaugh.headlong.rlp.exception;

/**
 * Indicates a failure to decode illegal or otherwise undecodeable data as per the RLP spec.
 */
public abstract class DecodeException extends Exception {

    DecodeException(String msg) {
        super(msg);
    }

    DecodeException(Throwable cause) {
        super(cause);
    }

    public abstract boolean isRecoverable();
}
