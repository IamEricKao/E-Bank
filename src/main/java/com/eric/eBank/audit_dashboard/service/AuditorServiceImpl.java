package com.eric.eBank.audit_dashboard.service;

import com.eric.eBank.account.dtos.AccountDTO;
import com.eric.eBank.account.repo.AccountRepo;
import com.eric.eBank.auth_users.dtos.UserDTO;
import com.eric.eBank.auth_users.repo.UserRepo;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import com.eric.eBank.transaction.repo.TransactionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditorServiceImpl implements AuditorService {

    private final UserRepo userRepo;
    private final AccountRepo accountRepo;
    private final TransactionRepo transactionRepo;
    private final ModelMapper modelMapper;

    @Override
    public Map<String, Long> getSystemTotals() {
        Long totalUsers = userRepo.count();
        Long totalAccounts = accountRepo.count();
        Long totalTransactions = transactionRepo.count();

        return Map.of(
                "totalUsers", totalUsers,
                "totalAccounts", totalAccounts,
                "totalTransactions", totalTransactions
        );
    }

    @Override
    public Optional<UserDTO> findUserByEmail(String email) {

        return userRepo.findByEmail(email)
                .map(user -> {
                    UserDTO userDTO = modelMapper.map(user, UserDTO.class);

                    if (userDTO.getAccounts() != null) {
                        userDTO.getAccounts().forEach(account -> {
                            if (account.getAccountType() != null) {
                                account.setAccountTypeName(account.getAccountType().getChinese());
                            }
                        });
                    }

                    return userDTO;
                });
    }

    @Override
    public Optional<AccountDTO> findAccountDetailsByAccountNumber(String accountNumber) {

        return accountRepo.findByAccountNumber(accountNumber)
                .map(account -> {
                    AccountDTO accountDTO = modelMapper.map(account, AccountDTO.class);

                    if (accountDTO.getAccountType() != null) {
                        accountDTO.setAccountTypeName(accountDTO.getAccountType().getChinese());
                    }

                    return accountDTO;
                });
    }

    @Override
    public List<TransactionDTO> findTransactionsByAccountNumber(String accountNumber) {

        return transactionRepo.findByAccount_AccountNumber(accountNumber).stream()
                .map(transaction -> {
                    TransactionDTO transactionDTOs = modelMapper.map(transaction, TransactionDTO.class);

                    if (transactionDTOs != null) {
                        if (transactionDTOs.getTransactionType() != null) {
                            transactionDTOs.setTransactionTypeName(transactionDTOs.getTransactionType().getChinese());
                        }
                    }

                    return transactionDTOs;
                })
                .toList();
    }

    @Override
    public Optional<TransactionDTO> findTransactionById(Long transactionId) {

        return transactionRepo.findById(transactionId)
                .map(transaction -> {
                    TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);

                    if (transactionDTO.getTransactionType() != null) {
                        transactionDTO.setTransactionTypeName(transactionDTO.getTransactionType().getChinese());
                    }
                    
                    return transactionDTO;
                });
    }
}
