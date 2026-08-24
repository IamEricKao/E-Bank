package com.eric.eBank.audit_dashboard.controller;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.audit_dashboard.service.AuditorService;
import com.eric.eBank.auth_users.dtos.UserDTO;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
public class AuditorController {

    private final AuditorService auditorService;

    @PreAuthorize("hasAuthority('AUDITOR') OR hasAuthority('ADMIN')")
    @GetMapping("/totals")
    public ResponseEntity<Map<String, Long>> getSystemTotals() {

        return ResponseEntity.ok(auditorService.getSystemTotals());
    }

    @PreAuthorize("hasAuthority('AUDITOR') OR hasAuthority('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<UserDTO> findUserByEmail(@RequestParam String email) {

        Optional<UserDTO> userDTO = auditorService.findUserByEmail(email);


        return userDTO.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/accounts")
    public ResponseEntity<AccountDTO> findAccountDetailsByAccountNumber(@RequestParam String accountNumber) {

        Optional<AccountDTO> accountDTO = auditorService.findAccountDetailsByAccountNumber(accountNumber);

        return accountDTO.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PreAuthorize("hasAuthority('AUDITOR') OR hasAuthority('ADMIN')")
    @GetMapping("/transactions/by-account")
    public ResponseEntity<List<TransactionDTO>> findTransactionsByAccountNumber(@RequestParam String accountNumber) {

        List<TransactionDTO> transactionDTOList = auditorService.findTransactionsByAccountNumber(accountNumber);

        if (transactionDTOList.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(transactionDTOList);
    }

    @PreAuthorize("hasAuthority('AUDITOR') OR hasAuthority('ADMIN')")
    @GetMapping("/transactions/by-id")
    public ResponseEntity<TransactionDTO> findTransactionById(@RequestParam Long transactionId) {

        Optional<TransactionDTO> transactionDTO = auditorService.findTransactionById(transactionId);

        return transactionDTO.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
