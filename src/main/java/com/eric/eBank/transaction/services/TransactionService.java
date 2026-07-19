package com.eric.eBank.transaction.services;

import com.eric.eBank.res.Response;
import com.eric.eBank.transaction.dtos.TransactionDTO;

import java.util.List;


public interface TransactionService {
    Response<?> createTransaction(TransactionDTO transactionDTO);

    Response<List<TransactionDTO>> getTransactionsFromAccount(String accountNumber, int page, int size);
}
