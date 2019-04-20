package com.esaulpaugh.headlong.rlp.exception;

/**
 * Indicates a failure to decode an RLP item due to a short buffer, potentially because the item has not finished
 * streaming, i.e. it is a prefix of some hypothetical, valid longer item.
 */
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
