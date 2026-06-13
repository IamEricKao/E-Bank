package com.eric.eBank.exceptions;

public class InvalidTransactionException extends RuntimeException{
    public InvalidTransactionException(String error) {
        super(error);
    }
}
