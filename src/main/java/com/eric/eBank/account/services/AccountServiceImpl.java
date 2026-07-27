package com.eric.eBank.account.services;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.account.entity.Account;
import com.eric.eBank.account.repo.AccountRepo;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.services.UserService;
import com.eric.eBank.enums.AccountStatus;
import com.eric.eBank.enums.AccountType;
import com.eric.eBank.enums.Currency;
import com.eric.eBank.exceptions.BadRequestException;
import com.eric.eBank.exceptions.NotFoundException;
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
        log.info("createAccount, 帳戶類別: {}, 建立者: {}", accountType, user.getEmail());

        String accountNumber = generateAccountNumber(accountType);

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .accountType(accountType)
                .user(user)
                .currency(Currency.TWD)
                .accountStatus(AccountStatus.ACTIVE)
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
        User user = userService.getCurrentLoggedInUser();

        var account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("帳戶不存在"));

        if (user.getAccounts().stream().noneMatch(a -> a.getAccountNumber().equals(accountNumber))) {
            throw new BadRequestException("您沒有權限關閉此帳戶");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("帳戶仍有餘額，無法關閉");
        }

        account.setAccountStatus(AccountStatus.CLOSED);
        account.setCloseAt(LocalDateTime.now());
        account.setCloseAt(LocalDateTime.now());
        accountRepo.save(account);

        return Response.builder()
                .statusCode(HttpStatusCode.OK)
                .message("帳戶已成功關閉")
                .build();
    }

    private String generateAccountNumber(AccountType accountType) {
        String accountNumber;

        do {
            // 以003開頭 + accountType的代碼 + 7位隨機數字
            accountNumber = "003" + accountType.getCode() + (random.nextInt(9000000) + 1000000);

        } while (accountRepo.findByAccountNumber(accountNumber).isPresent());

        log.info("generateAccountNumber, 帳戶號碼: {}", accountNumber);

        return accountNumber;
    }
}
