package com.tproject.workshop.exception;

public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 4315420350487571745L;

    public NotFoundException(String message){
        super(message);
    }
    public NotFoundException(String message, Exception exception){
        super(message, exception);
    }
    public NotFoundException(String message, Object... messageParams){
        super(String.format(message, messageParams));
    }
}
