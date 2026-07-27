package com.eric.eBank.transaction.services;

import com.eric.eBank.account.entity.Account;
import com.eric.eBank.account.repo.AccountRepo;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.services.UserService;
import com.eric.eBank.enums.TransactionStatus;
import com.eric.eBank.enums.TransactionType;
import com.eric.eBank.exceptions.InsufficientBalanceException;
import com.eric.eBank.exceptions.InvalidTransactionException;
import com.eric.eBank.exceptions.NotFoundException;
import com.eric.eBank.notification.repo.NotificationRepo;
import com.eric.eBank.res.Response;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import com.eric.eBank.transaction.dtos.TransactionRequest;
import com.eric.eBank.transaction.entity.Transaction;
import com.eric.eBank.transaction.repo.TransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepo transactionRepo;
    private final AccountRepo accountRepo;
    private final NotificationRepo notificationRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Response<?> createTransaction(TransactionRequest transactionRequest) {

        TransactionType transactionType = transactionRequest.getTransactionType();

        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionType);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDescription(transactionRequest.getDescription());

        switch (transactionType) {
            case DEPOSIT -> handleDeposit(transactionRequest, transaction);
            case WITHDRAWAL -> handleWithdrawal(transactionRequest, transaction);
            case TRANSFER -> handleTransfer(transactionRequest, transaction);
            default -> throw new InvalidTransactionException("無效的交易類型: " + transactionType);
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        Transaction savedTransaction = transactionRepo.save(transaction);

        // send email
        sendTransactionNotifications(savedTransaction);

        return Response.<Transaction>builder()
                .statusCode(HttpStatus.OK.value())
                .message("交易成功")
                .data(savedTransaction)
                .build();

    }

    @Override
    @Transactional
    public Response<List<TransactionDTO>> getTransactionsForMyAccount(String accountNumber, int page, int size) {
        return null;
    }

    private void handleDeposit(TransactionRequest transactionRequest, Transaction transaction) {

        Account account = accountRepo.findByAccountNumber(transactionRequest.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("找不到帳戶: " + transactionRequest.getAccountNumber()));

        account.setBalance(account.getBalance().add(transaction.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleWithdrawal(TransactionRequest transactionRequest, Transaction transaction) {

        Account account = accountRepo.findByAccountNumber(transactionRequest.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("找不到帳戶: " + transactionRequest.getAccountNumber()));

        if (account.getBalance().compareTo(transaction.getAmount()) < 0) {
            throw new InsufficientBalanceException("餘額不足，無法進行提款");
        }

        account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleTransfer(TransactionRequest transactionRequest, Transaction transaction) {

        String sourceAccountNumber = transactionRequest.getAccountNumber();
        String destinationAccountNumber = transactionRequest.getDestinationAccountNumber();

        Account sourceAccount = accountRepo.findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new NotFoundException("找不到轉出帳戶: " + sourceAccountNumber));

        Account destinationAccount = accountRepo.findByAccountNumber(destinationAccountNumber)
                .orElseThrow(() -> new NotFoundException("找不到轉入帳戶: " + destinationAccountNumber));

        if (sourceAccount.getBalance().compareTo(transactionRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException("餘額不足，無法進行轉帳");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(transactionRequest.getAmount()));
        accountRepo.save(sourceAccount);

        destinationAccount.setBalance(destinationAccount.getBalance().add(transactionRequest.getAmount()));
        accountRepo.save(destinationAccount);

        transaction.setAccount(sourceAccount);
        transaction.setSourceAccount(sourceAccountNumber);
        transaction.setDestinationAccount(destinationAccountNumber);
    }

    public void sendTransactionNotifications(Transaction transaction) {

        User user = transaction.getAccount().getUser();

        String subject;
        String template;

        Map<String, Object> templateVariables = Map.of(
                "name", user.getFirstName(),
                "amount", transaction.getAmount(),
                "accountNumber", transaction.getAccount().getAccountNumber(),
                "date", transaction.getTransactionDate(),
                "balance", transaction.getAccount().getBalance()
        );

        TransactionType transactionType = transaction.getTransactionType();
        if (transactionType == TransactionType.DEPOSIT) {
            subject = "存款通知";
            template = "credit-alter";


        }
    }
}
