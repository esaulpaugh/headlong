package com.esaulpaugh.headlong.rlp.exception;

/**
 * Indicates a failure to decode an RLP item that is unrecoverably malformed or exceeds the bounds of its parent item.
 */
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
