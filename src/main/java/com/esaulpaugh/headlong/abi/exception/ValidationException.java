package com.esaulpaugh.headlong.abi.exception;

public class ValidationException extends Exception {

    public ValidationException(String msg) {
        super(msg);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public ValidationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
