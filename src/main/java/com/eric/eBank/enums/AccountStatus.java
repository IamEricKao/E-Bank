package com.eric.eBank.enums;

public enum AccountStatus {
    ACTIVE("開通"),
    SUSPENDED("凍結"),
    CLOSED("關閉");

    private final String chinese;

    AccountStatus(String chinese){ this.chinese = chinese; }

    public String getChinese() {
        return chinese;
    }
}
