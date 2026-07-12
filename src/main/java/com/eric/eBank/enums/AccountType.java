package com.eric.eBank.enums;

public enum AccountType {
    // SAVINGS
    儲蓄存款("01"),
    // CURRENT
    活期存款("02");

    private String code;

    AccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
