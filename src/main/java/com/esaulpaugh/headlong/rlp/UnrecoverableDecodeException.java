package com.esaulpaugh.headlong.rlp;

public class UnrecoverableDecodeException extends DecodeException {

    public UnrecoverableDecodeException(String msg) {
        super(msg);
    }

    public UnrecoverableDecodeException(Throwable cause) {
        super(cause);
    }

    @Override
    boolean isRecoverable() {
        return false;
    }
}
