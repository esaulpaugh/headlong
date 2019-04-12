package com.esaulpaugh.headlong.rlp.exception;

public class RecoverableDecodeException extends DecodeException {

    public RecoverableDecodeException(String msg) {
        super(msg);
    }

    public RecoverableDecodeException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean isRecoverable() {
        return true;
    }
}
