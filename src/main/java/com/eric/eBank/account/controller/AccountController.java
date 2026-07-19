package com.eric.eBank.account.controller;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.account.services.AccountService;
import com.eric.eBank.res.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<Response<List<AccountDTO>>> getAccount() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }

    @GetMapping("/close/{accountNumber}")
    public ResponseEntity<Response<?>> closeAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.closeAccount(accountNumber));
    }

}
