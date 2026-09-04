package com.syfe.financemanager.service;

import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.transaction.TransactionListResponse;
import com.syfe.financemanager.dto.transaction.TransactionRequest;
import com.syfe.financemanager.dto.transaction.TransactionResponse;
import com.syfe.financemanager.dto.transaction.TransactionUpdateRequest;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;

import java.time.LocalDate;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest request, User user);
    TransactionListResponse getTransactions(User user, LocalDate startDate, LocalDate endDate, String categoryName, Long categoryId, TransactionType type);
    TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request, User user);
    MessageResponse deleteTransaction(Long id, User user);
}
