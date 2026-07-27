package com.eric.eBank.enums;

public enum AccountType {
    // 儲蓄存款("01")
    SAVINGS("01"),
    // 活期存款("02")
    CURRENT("02");


    private String code;

    AccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
