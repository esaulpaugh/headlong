package com.esaulpaugh.headlong.rlp.codec.exception;

public class DecodeException extends Exception {

    public DecodeException(String msg) {
        super(msg);
    }

    public DecodeException(Throwable cause) {
        super(cause);
    }

}
