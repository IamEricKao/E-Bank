package com.eric.eBank.exceptions;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String error) {
        super(error);
    }
}
