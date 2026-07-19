package com.eric.eBank.transaction.services;

import com.eric.eBank.res.Response;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    @Override
    public Response<?> createTransaction(TransactionDTO transactionDTO) {
        return null;
    }

    @Override
    public Response<List<TransactionDTO>> getTransactionsFromAccount(String accountNumber, int page, int size) {
        return null;
    }
}
