package com.eric.eBank.transaction.dtos;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.account.entity.Account;
import com.eric.eBank.enums.TransactionStatus;
import com.eric.eBank.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDTO {
    private Long id;
    private BigDecimal amount;
    private TransactionType transactionType;
    private LocalDateTime transactionDate = LocalDateTime.now();
    private String description;
    private TransactionStatus transactionStatus;
    @JsonBackReference
    private AccountDTO account;
    private String sourceAccount;
    private String destinationAccount;
}
