package com.esaulpaugh.headlong.rlp;

public class RecoverableDecodeException extends DecodeException {

    public RecoverableDecodeException(String msg) {
        super(msg);
    }

    public RecoverableDecodeException(Throwable cause) {
        super(cause);
    }

    @Override
    boolean isRecoverable() {
        return true;
    }
}
