package com.eric.eBank.account.services;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.account.entity.Account;
import com.eric.eBank.account.repo.AccountRepo;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.services.UserService;
import com.eric.eBank.enums.AccountStatus;
import com.eric.eBank.enums.AccountType;
import com.eric.eBank.enums.Currency;
import com.eric.eBank.res.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.http.HttpStatusCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;

    private final Random random = new Random();

    @Override
    public Account createAccount(AccountType accountType, User user) {
        String accountNumber = generateAccountNumber(accountType);

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .accountType(accountType)
                .user(user)
                .currency(Currency.台幣)
                .accountStatus(AccountStatus.正常)
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepo.save(account);
    }

    @Override
    public Response<List<AccountDTO>> getMyAccounts() {
        User user = userService.getCurrentLoggedInUser();
        List<AccountDTO> accountDTOs = accountRepo.findByUserId(user.getId())
                .stream()
                .map(account -> modelMapper.map(account, AccountDTO.class))
                .toList();

        return Response.<List<AccountDTO>>builder()
                .statusCode(HttpStatusCode.OK)
                .message("成功取得帳戶列表")
                .data(accountDTOs)
                .build();
    }

    @Override
    public Response<?> closeAccount(String accountNumber) {
        return null;
    }

    private String generateAccountNumber(AccountType accountType) {
        String accountNumber;

        do {
            // 以003開頭 + accountType的代碼 + 7位隨機數字
            accountNumber = "003" + accountType.getCode() + (random.nextInt(9000000) + 1000000);

        } while (accountRepo.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }
}
