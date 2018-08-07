package com.esaulpaugh.headlong.rlp;

public class DecodeException extends Exception {

    public DecodeException(String msg) {
        super(msg);
    }

    public DecodeException(Throwable cause) {
        super(cause);
    }

}
