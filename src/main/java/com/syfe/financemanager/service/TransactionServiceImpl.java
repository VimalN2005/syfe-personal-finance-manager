package com.syfe.financemanager.service;

import com.syfe.financemanager.common.BadRequestException;
import com.syfe.financemanager.common.ForbiddenException;
import com.syfe.financemanager.common.ResourceNotFoundException;
import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.transaction.TransactionListResponse;
import com.syfe.financemanager.dto.transaction.TransactionRequest;
import com.syfe.financemanager.dto.transaction.TransactionResponse;
import com.syfe.financemanager.dto.transaction.TransactionUpdateRequest;
import com.syfe.financemanager.entity.Category;
import com.syfe.financemanager.entity.Transaction;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.repository.CategoryRepository;
import com.syfe.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, User user) {
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transaction date cannot be in the future");
        }

        Category category = categoryRepository.findByNameAccessibleByUser(request.getCategory().trim(), user)
                .orElseThrow(() -> new BadRequestException("Category not found or not accessible: " + request.getCategory()));

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .date(request.getDate())
                .category(category)
                .description(request.getDescription())
                .user(user)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(User user, LocalDate startDate, LocalDate endDate,
                                                  String categoryName, Long categoryId, TransactionType type) {
        List<Transaction> transactions = transactionRepository.findByUserOrderByDateDescIdDesc(user)
                .stream()
                .filter(t -> startDate == null || !t.getDate().isBefore(startDate))
                .filter(t -> endDate == null || !t.getDate().isAfter(endDate))
                .filter(t -> categoryName == null || t.getCategory().getName().equalsIgnoreCase(categoryName.trim()))
                .filter(t -> categoryId == null || t.getCategory().getId().equals(categoryId))
                .filter(t -> type == null || t.getCategory().getType() == type)
                .toList();

        List<TransactionResponse> responses = transactions.stream()
                .map(this::mapToResponse)
                .toList();

        return TransactionListResponse.builder().transactions(responses).build();
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to modify this transaction");
        }

        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            Category category = categoryRepository.findByNameAccessibleByUser(request.getCategory().trim(), user)
                    .orElseThrow(() -> new BadRequestException("Category not found or not accessible: " + request.getCategory()));
            transaction.setCategory(category);
        }

        // Business Rule: Transaction date is immutable and is deliberately ignored if sent.

        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public MessageResponse deleteTransaction(Long id, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to delete this transaction");
        }

        transactionRepository.delete(transaction);
        return MessageResponse.builder().message("Transaction deleted successfully").build();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .date(transaction.getDate())
                .category(transaction.getCategory().getName())
                .description(transaction.getDescription())
                .type(transaction.getCategory().getType())
                .build();
    }
}
