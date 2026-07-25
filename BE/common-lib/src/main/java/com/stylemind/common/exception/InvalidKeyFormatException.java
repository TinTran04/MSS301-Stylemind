package com.stylemind.common.exception;

public class InvalidKeyFormatException extends CryptoException {
    public InvalidKeyFormatException(String message) {
        super(message);
    }
    public InvalidKeyFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
