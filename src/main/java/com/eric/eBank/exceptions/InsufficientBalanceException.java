package com.eric.eBank.exceptions;

public class InsufficientBalanceException extends RuntimeException{
    public InsufficientBalanceException(String error) {
        super(error);
    }
}
