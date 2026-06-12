package com.eric.eBank.transaction.dtos;

import com.eric.eBank.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {
    private TransactionType transactionType;
    private BigDecimal amount;
    private String accountNumber;
    private String description;

    // 轉帳時的收款帳號
    private String destinationAccountNumber;
}
