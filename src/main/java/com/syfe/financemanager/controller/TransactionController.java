package com.syfe.financemanager.controller;

import com.syfe.financemanager.dto.auth.MessageResponse;
import com.syfe.financemanager.dto.transaction.TransactionListResponse;
import com.syfe.financemanager.dto.transaction.TransactionRequest;
import com.syfe.financemanager.dto.transaction.TransactionResponse;
import com.syfe.financemanager.dto.transaction.TransactionUpdateRequest;
import com.syfe.financemanager.entity.TransactionType;
import com.syfe.financemanager.entity.User;
import com.syfe.financemanager.service.AuthService;
import com.syfe.financemanager.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Management", description = "CRUD and filtering operations for financial transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;

    public TransactionController(TransactionService transactionService, AuthService authService) {
        this.transactionService = transactionService;
        this.authService = authService;
    }

    @PostMapping
    @Operation(summary = "Create a financial transaction")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        TransactionResponse response = transactionService.createTransaction(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Retrieve transactions with optional date, category, and type filters")
    public ResponseEntity<TransactionListResponse> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        TransactionListResponse response = transactionService.getTransactions(currentUser, startDate, endDate, category, categoryId, type);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing transaction (date field is immutable)")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        TransactionResponse response = transactionService.updateTransaction(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<MessageResponse> deleteTransaction(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        MessageResponse response = transactionService.deleteTransaction(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
