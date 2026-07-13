package com.stylemind.common.exception;

public class KeyLoadException extends CryptoException {
    public KeyLoadException(String message) {
        super(message);
    }
    public KeyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
