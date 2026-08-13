package com.eric.eBank.enums;

public enum TransactionType {
    DEPOSIT("存款"),
    WITHDRAWAL("提款"),
    TRANSFER("提款");

    private final String chinese;

    TransactionType(String chinese){ this.chinese = chinese; }

    public String getChinese() {
        return chinese;
    }
}
