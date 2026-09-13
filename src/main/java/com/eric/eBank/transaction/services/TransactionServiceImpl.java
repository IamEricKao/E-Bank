package com.eric.eBank.transaction.services;

import com.eric.eBank.account.entity.Account;
import com.eric.eBank.account.repo.AccountRepo;
import com.eric.eBank.auth_users.entity.User;
import com.eric.eBank.auth_users.services.UserService;
import com.eric.eBank.enums.EntryDirection;
import com.eric.eBank.enums.TransactionStatus;
import com.eric.eBank.enums.TransactionType;
import com.eric.eBank.exceptions.*;
import com.eric.eBank.notification.dtos.NotificationDTO;
import com.eric.eBank.notification.services.NotificationService;
import com.eric.eBank.res.Response;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import com.eric.eBank.transaction.dtos.TransactionRequest;
import com.eric.eBank.transaction.entity.Transaction;
import com.eric.eBank.transaction.idempotency.dtos.TransferResultDTO;
import com.eric.eBank.transaction.idempotency.entity.TransferIdempotencyRecord;
import com.eric.eBank.transaction.idempotency.repo.TransferIdempotencyRecordRepo;
import com.eric.eBank.transaction.repo.TransactionRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepo transactionRepo;
    private final AccountRepo accountRepo;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final TransferIdempotencyRecordRepo idempotencyRepo;

    @Override
    @Transactional
    public Response<?> createTransaction(TransactionRequest transactionRequest, String idempotencyKey) {

        if (transactionRequest.getTransactionType() != TransactionType.TRANSFER) {
            return transactionTemplate.execute(status ->
                    createNonTransferTransaction(transactionRequest)
            );
        }

        return createIdempotentTransfer(transactionRequest, idempotencyKey);
    }

    private Response<?> createNonTransferTransaction(TransactionRequest transactionRequest) {

        TransactionType transactionType = transactionRequest.getTransactionType();
        Transaction savedTransaction;

        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionType);
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDescription(transactionRequest.getDescription());

        switch (transactionType) {
            case DEPOSIT -> {
                transaction.setEntryDirection(EntryDirection.CREDIT);
                handleDeposit(transactionRequest, transaction);
            }
            case WITHDRAWAL -> {
                transaction.setEntryDirection(EntryDirection.DEBIT);
                handleWithdrawal(transactionRequest, transaction);
            }
            default -> throw new InvalidTransactionException("無效的交易類型: " + transactionType);
        }

        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        savedTransaction = transactionRepo.save(transaction);

        // send email
        sendTransactionNotifications(savedTransaction);

        return Response.<Transaction>builder()
                .statusCode(HttpStatus.OK.value())
                .message("交易成功")
                .build();
    }

    private Response<?> createIdempotentTransfer(TransactionRequest transactionRequest, String idempotencyKey) {

        String key = validateIdempotencyKey(idempotencyKey);
        String requestHash = calculateRequestHash(transactionRequest);
        User user = userService.getCurrentLoggedInUser();

        Optional<TransferIdempotencyRecord> existing = idempotencyRepo.findByUserIdAndIdempotencyKey(
                user.getId(), key);

        if (existing.isPresent()) {
            return replayOrReject(existing.get(), requestHash);
        }

        try {
            return transactionTemplate.execute(status -> {

                TransferIdempotencyRecord record = new TransferIdempotencyRecord();
                record.setUser(user);
                record.setIdempotencyKey(key);
                record.setRequestHash(requestHash);

                idempotencyRepo.saveAndFlush(record);

                Transaction debitTransaction = handleTransfer(transactionRequest);

                Long transferId = debitTransaction.getId();
                record.setTransferId(transferId);
                idempotencyRepo.save(record);

                // 只在第一次成功執行轉帳時才發送通知
                sendTransactionNotifications(debitTransaction);

                return buildTransferResponse(transferId);
            });
        } catch (DataIntegrityViolationException ex) {
            TransferIdempotencyRecord winner = idempotencyRepo.findByUserIdAndIdempotencyKey(user.getId(), key)
                    .orElseThrow(() -> ex);
            return replayOrReject(winner, requestHash);
        }
    }

    private String validateIdempotencyKey(String value) {

        if (value == null || value.isEmpty()) {
            throw new BadRequestException("轉帳請求缺少 Idempotency-Key");
        }

        try {
            String normalized = UUID.fromString(value).toString();

            if (!normalized.equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }

            return normalized;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Idempotency-Key 必須是合法的 UUID");
        }
    }

    private String calculateRequestHash(TransactionRequest request) {

        try {
            Map<String, Object> values = new TreeMap<>();
            values.put("transactionType", request.getTransactionType());
            values.put("accountNumber", request.getAccountNumber());
            values.put("destinationAccountNumber", request.getDestinationAccountNumber());
            values.put("amount", request.getAmount().stripTrailingZeros().toPlainString());
            values.put("description", request.getDescription());

            byte[] json = objectMapper.writeValueAsBytes(values);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json);

            return HexFormat.of().formatHex(hash);
        } catch (JacksonException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("無法產生轉帳請求hash", ex);
        }
    }

    private Response<?> replayOrReject(TransferIdempotencyRecord record, String requestHash) {

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("相同 Idempotency-key 不可用於不同的轉帳內容");
        }

        return buildTransferResponse(record.getTransferId());
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
        List<TransactionDTO> transactionDTOs = txns.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO.class))
                .toList();
        if (!transactionDTOs.isEmpty()) {
            transactionDTOs.forEach(transactionDTO -> {
                transactionDTO.setTransactionTypeName(transactionDTO.getTransactionType().getChinese());
            });
        }

        return Response.<List<TransactionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("交易紀錄，查詢成功")
                .data(transactionDTOs)
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

    private Transaction handleTransfer(TransactionRequest transactionRequest) {

        String srcAccountNumber = transactionRequest.getAccountNumber();
        String destAccountNumber = transactionRequest.getDestinationAccountNumber();

        if (srcAccountNumber == null || destAccountNumber == null) {
            throw new BadRequestException("轉出或轉入帳號不可為空");
        }

        if (srcAccountNumber.equals(destAccountNumber)) {
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
        destinationAccount.setBalance(destinationAccount.getBalance().add(transactionRequest.getAmount()));

        accountRepo.save(sourceAccount);
        accountRepo.save(destinationAccount);

        String transferRef = UUID.randomUUID().toString();

        Transaction debitTxn = createTransferTxn(
                transactionRequest,
                sourceAccount,
                EntryDirection.DEBIT,
                transferRef
        );

        Transaction creditTxn = createTransferTxn(
                transactionRequest,
                destinationAccount,
                EntryDirection.CREDIT,
                transferRef
        );

        transactionRepo.saveAll(List.of(debitTxn, creditTxn));

        return debitTxn;
    }

    private Transaction createTransferTxn(
            TransactionRequest transactionRequest,
            Account account,
            EntryDirection entryDirection,
            String transferRef
    ) {
        return Transaction.builder()
                .amount(transactionRequest.getAmount())
                .transactionType(TransactionType.TRANSFER)
                .description(transactionRequest.getDescription())
                .transactionStatus(TransactionStatus.SUCCESS)
                .account(account)
                .sourceAccount(transactionRequest.getAccountNumber())
                .destinationAccount(transactionRequest.getDestinationAccountNumber())
                .entryDirection(entryDirection)
                .transferReference(transferRef)
                .build();
    }

    private Response<TransferResultDTO> buildTransferResponse(Long transferId) {
        return Response.<TransferResultDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("交易成功")
                .data(TransferResultDTO.builder()
                        .transferId(transferId)
                        .status(TransactionStatus.SUCCESS)
                        .build())
                .build();
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
