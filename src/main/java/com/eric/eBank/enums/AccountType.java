package com.eric.eBank.enums;

public enum AccountType {

    SAVINGS("01", "儲蓄存款"),

    CURRENT("02", "活期存款");


    private String code;
    private String chinese;

    AccountType(String code, String chinese) {
        this.code = code;
        this.chinese = chinese;
    }

    public String getCode() {
        return code;
    }

    public String getChinese() {
        return chinese;
    }
}
