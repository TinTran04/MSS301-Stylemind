package com.stylemind.common.exception;

public class KeyDecodingException extends CryptoException {
    public KeyDecodingException(String message) {
        super(message);
    }
    public KeyDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
