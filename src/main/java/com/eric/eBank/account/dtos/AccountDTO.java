package com.eric.eBank.account.dtos;

import com.eric.eBank.auth_users.dtos.UserDTO;
import com.eric.eBank.enums.AccountStatus;
import com.eric.eBank.enums.AccountType;
import com.eric.eBank.enums.Currency;
import com.eric.eBank.transaction.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {

    private Long id;

    private String accountNumber;

    private BigDecimal balance;

    private AccountType accountType;

    @JsonBackReference
    private UserDTO user;

    private Currency currency;

    private AccountStatus accountStatus;

    @JsonManagedReference
    private List<TransactionDTO> transactions;

    private LocalDateTime closeAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
