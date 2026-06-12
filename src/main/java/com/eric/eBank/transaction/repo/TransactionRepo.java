package com.eric.eBank.transaction.repo;

import com.eric.eBank.account.entity.Account;
import com.eric.eBank.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByAccount_AccountNumber(String account, Pageable pageable);

    List<Transaction> findByAccount_AccountNumber(String accountNumber);
}
