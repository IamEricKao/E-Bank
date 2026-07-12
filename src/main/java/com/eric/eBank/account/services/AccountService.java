package com.eric.eBank.account.services;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.account.entity.Account;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.enums.AccountType;
import com.eric.eBank.res.Response;

import java.util.List;

public interface AccountService {
    Account createAccount(AccountType accountType, User user);

    Response<List<AccountDTO>> getMyAccounts();

    Response<?> closeAccount(String accountNumber);
}
