package com.eric.eBank.transaction.services;

import com.eric.eBank.account.entity.Account;
import com.eric.eBank.account.repo.AccountRepo;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.services.UserService;
import com.eric.eBank.enums.TransactionStatus;
import com.eric.eBank.enums.TransactionType;
import com.eric.eBank.exceptions.BadRequestException;
import com.eric.eBank.exceptions.InsufficientBalanceException;
import com.eric.eBank.exceptions.InvalidTransactionException;
import com.eric.eBank.exceptions.NotFoundException;
import com.eric.eBank.notification.dtos.NotificationDTO;
import com.eric.eBank.notification.services.NotificationService;
import com.eric.eBank.res.Response;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import com.eric.eBank.transaction.dtos.TransactionRequest;
import com.eric.eBank.transaction.entity.Transaction;
import com.eric.eBank.transaction.repo.TransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepo transactionRepo;
    private final AccountRepo accountRepo;
    private final NotificationService notificationService;
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
                .build();

    }

    @Override
    @Transactional
    public Response<List<TransactionDTO>> getTransactionsForMyAccount(String accountNumber, int page, int size) {

        User user = userService.getCurrentLoggedInUser();

        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("找不到帳戶: " + accountNumber));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("您無權查看此帳戶的交易紀錄");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepo.findByAccount_AccountNumber(accountNumber, pageable);
        List<TransactionDTO> transactionDTOS = txns.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO.class))
                .toList();

        return Response.<List<TransactionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("交易紀錄，查詢成功")
                .data(transactionDTOS)
                .meta(Map.of(
                        "currentPage", txns.getNumber(),
                        "totalItems", txns.getTotalElements(),
                        "totalPages", txns.getTotalPages(),
                        "pageSize", txns.getSize()
                ))
                .build();
    }

    private void handleDeposit(TransactionRequest transactionRequest, Transaction transaction) {

        Account account = accountRepo.findByAccountNumberForUpdate(transactionRequest.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("找不到帳戶: " + transactionRequest.getAccountNumber()));

        account.setBalance(account.getBalance().add(transaction.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleWithdrawal(TransactionRequest transactionRequest, Transaction transaction) {

        Account account = accountRepo.findByAccountNumberForUpdate(transactionRequest.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("找不到帳戶: " + transactionRequest.getAccountNumber()));

        if (account.getBalance().compareTo(transaction.getAmount()) < 0) {
            throw new InsufficientBalanceException("餘額不足，無法進行提款");
        }

        account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleTransfer(TransactionRequest transactionRequest, Transaction transaction) {

        String srcAccountNumber = transactionRequest.getAccountNumber();
        String destAccountNumber = transactionRequest.getDestinationAccountNumber();

        if(srcAccountNumber == null || destAccountNumber == null) {
            throw new BadRequestException("轉出或轉入帳號不可為空");
        }

        if(srcAccountNumber.equals(destAccountNumber)) {
            throw new BadRequestException("不可轉帳至同一個帳戶");
        }

        boolean order = srcAccountNumber.compareTo(destAccountNumber) < 0;
        String firstAccountNumber = order ? srcAccountNumber : destAccountNumber;
        String secAccountNumber = order ? destAccountNumber : srcAccountNumber;

        Account firstAccount = accountRepo.findByAccountNumberForUpdate(firstAccountNumber)
                .orElseThrow(() -> new NotFoundException("找不到付款帳戶: " + firstAccountNumber));

        Account secAccount = accountRepo.findByAccountNumberForUpdate(secAccountNumber)
                .orElseThrow(() -> new NotFoundException("找不到收款帳戶: " + secAccountNumber));

        Account sourceAccount = firstAccountNumber.equals(srcAccountNumber) ? firstAccount : secAccount;
        Account destinationAccount = firstAccountNumber.equals(srcAccountNumber) ? secAccount : firstAccount;

        if (sourceAccount.getBalance().compareTo(transactionRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException("餘額不足，無法進行轉帳");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(transactionRequest.getAmount()));
        accountRepo.save(sourceAccount);

        destinationAccount.setBalance(destinationAccount.getBalance().add(transactionRequest.getAmount()));
        accountRepo.save(destinationAccount);

        transaction.setAccount(sourceAccount);
        transaction.setSourceAccount(srcAccountNumber);
        transaction.setDestinationAccount(destAccountNumber);
    }

    public void sendTransactionNotifications(Transaction transaction) {

        User user = transaction.getAccount().getUser();

        String subject;
        String template;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> templateVariables = Map.of(
                "name", user.getFirstName(),
                "amount", transaction.getAmount(),
                "accountNumber", transaction.getAccount().getAccountNumber(),
                "date", transaction.getTransactionDate().format(formatter),
                "balance", transaction.getAccount().getBalance()
        );

        TransactionType transactionType = transaction.getTransactionType();
        if (transactionType == TransactionType.DEPOSIT) {

            subject = "入帳通知";
            template = "credit-alert";

            NotificationDTO notificationDTO = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationDTO, user);
        } else if (transactionType == TransactionType.WITHDRAWAL) {

            subject = "出帳通知";
            template = "debit-alert";

            NotificationDTO notificationDTO = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationDTO, user);
        } else if (transactionType == TransactionType.TRANSFER) {

            // region 寄送轉帳通知給轉出帳戶
            subject = "出帳通知";
            template = "debit-alert";

            NotificationDTO sourceNotificationDTO = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(sourceNotificationDTO, user);
            // endregion 寄送轉帳通知給轉出帳戶

            // region 寄送轉帳通知給轉入帳戶
            String destSubject = "入帳通知";
            String destTemplate = "credit-alert";

            Account destAccount = accountRepo.findByAccountNumber(transaction.getDestinationAccount())
                    .orElseThrow(() -> new NotFoundException("找不到收款帳戶: " + transaction.getDestinationAccount()));
            User destUser = destAccount.getUser();

            Map<String, Object> destTemplateVariables = Map.of(
                    "name", destUser.getFirstName(),
                    "amount", transaction.getAmount(),
                    "accountNumber", destAccount.getAccountNumber(),
                    "date", transaction.getTransactionDate().format(formatter),
                    "balance", destAccount.getBalance()
            );

            NotificationDTO destNotificationDTO = NotificationDTO.builder()
                    .recipient(destUser.getEmail())
                    .subject(destSubject)
                    .templateName(destTemplate)
                    .templateVariables(destTemplateVariables)
                    .build();

            notificationService.sendEmail(destNotificationDTO, destUser);
            // endregion 寄送轉帳通知給轉入帳戶
        }
    }
}
