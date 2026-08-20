package com.picpaysimplificado.service;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.transaction.TransactionRepository;
import com.picpaysimplificado.domain.transaction.TransactionStatus;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.domain.user.UserType;
import com.picpaysimplificado.dto.NotificationPayload;
import com.picpaysimplificado.dto.TransferRequest;
import com.picpaysimplificado.dto.TransferResponse;
import com.picpaysimplificado.infra.exception.InsufficientBalanceException;
import com.picpaysimplificado.infra.exception.TransactionNotAllowedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final TransactionRepository transactionRepository;

    /**
     * Executes a money transfer between two users.
     *
     * Business rules:
     * 1. Payer must exist
     * 2. Payee must exist
     * 3. Payer cannot be a MERCHANT
     * 4. Payer must have sufficient balance
     * 5. Transfer must be authorized by external service
     * 6. Entire operation is wrapped in a DB transaction (ACID)
     * 7. Notification is sent asynchronously via RabbitMQ
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("Iniciando transferência: Payer={}, Payee={}, Value={}",
                request.payer(), request.payee(), request.value());

        // 1. Validate payer and payee exist
        User payer = userService.findById(request.payer());
        User payee = userService.findById(request.payee());

        // 2. Validate payer is not a merchant
        if (payer.getUserType() == UserType.MERCHANT) {
            throw new TransactionNotAllowedException(
                    "Lojistas não podem realizar transferências, apenas receber.");
        }

        // 3. Validate payer != payee
        if (payer.getId().equals(payee.getId())) {
            throw new TransactionNotAllowedException(
                    "Não é possível transferir dinheiro para si mesmo.");
        }

        // 4. Validate sufficient balance
        if (payer.getBalance().compareTo(request.value()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Saldo insuficiente. Saldo atual: R$ %s, Valor da transferência: R$ %s",
                            payer.getBalance(), request.value()));
        }

        // 5. Consult external authorization service
        authorizationService.authorize();

        // 6. Execute transfer (within @Transactional — automatic rollback on failure)
        payer.setBalance(payer.getBalance().subtract(request.value()));
        payee.setBalance(payee.getBalance().add(request.value()));

        userService.save(payer);
        userService.save(payee);

        // 7. Record transaction
        Transaction transaction = Transaction.builder()
                .amount(request.value())
                .sender(payer)
                .receiver(payee)
                .status(TransactionStatus.COMPLETED)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transferência concluída com sucesso. Transaction ID: {}", savedTransaction.getId());

        // 8. Send async notification via RabbitMQ
        try {
            NotificationPayload notificationPayload = new NotificationPayload(
                    savedTransaction.getId(),
                    payee.getId(),
                    payee.getEmail(),
                    payee.getFirstName() + " " + payee.getLastName(),
                    payer.getFirstName() + " " + payer.getLastName(),
                    request.value(),
                    LocalDateTime.now()
            );
            notificationService.sendNotification(notificationPayload);
        } catch (Exception e) {
            // Notification failure should NOT rollback the transfer
            log.error("Falha ao enviar notificação para a fila: {}", e.getMessage());
        }

        return new TransferResponse(
                savedTransaction.getId(),
                savedTransaction.getAmount(),
                payer.getId(),
                payer.getFirstName() + " " + payer.getLastName(),
                payee.getId(),
                payee.getFirstName() + " " + payee.getLastName(),
                savedTransaction.getStatus().name(),
                savedTransaction.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(t -> new TransferResponse(
                        t.getId(),
                        t.getAmount(),
                        t.getSender().getId(),
                        t.getSender().getFirstName() + " " + t.getSender().getLastName(),
                        t.getReceiver().getId(),
                        t.getReceiver().getFirstName() + " " + t.getReceiver().getLastName(),
                        t.getStatus().name(),
                        t.getCreatedAt()
                ))
                .toList();
    }
}
