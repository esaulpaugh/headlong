package com.esaulpaugh.headlong.rlp.exception;

public class UnrecoverableDecodeException extends DecodeException {

    public UnrecoverableDecodeException(String msg) {
        super(msg);
    }

    public UnrecoverableDecodeException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean isRecoverable() {
        return false;
    }
}
