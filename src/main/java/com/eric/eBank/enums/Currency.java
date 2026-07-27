package com.eric.eBank.enums;

public enum Currency {
    // 美金, 歐元, 台幣
    USD("美金"),
    EUR("歐元"),
    TWD("台幣");

    private String chinese;

    Currency(String chinese) {
        this.chinese = chinese;
    }

    public String getChinese() {
        return this.chinese;
    }
}
