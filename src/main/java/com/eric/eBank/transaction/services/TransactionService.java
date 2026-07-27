package com.eric.eBank.transaction.services;

import com.eric.eBank.res.Response;
import com.eric.eBank.transaction.dtos.TransactionDTO;
import com.eric.eBank.transaction.dtos.TransactionRequest;

import java.util.List;


public interface TransactionService {
    Response<?> createTransaction(TransactionRequest transactionRequest);

    Response<List<TransactionDTO>> getTransactionsForMyAccount(String accountNumber, int page, int size);
}
