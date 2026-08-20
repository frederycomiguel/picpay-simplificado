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
     * [PT-BR] Executa a transferência de dinheiro entre dois usuários aplicando todas as regras de negócio.
     *         Regras de Negócio:
     *         1. Pagador e recebedor devem existir no banco.
     *         2. Pagador não pode ser LOJISTA (MERCHANT).
     *         3. Pagador não pode transferir para si mesmo.
     *         4. Pagador deve possuir saldo suficiente (balance >= value).
     *         5. A transação deve ser autorizada por serviço HTTP externo.
     *         6. Débito e crédito são executados em transação ACID (@Transactional com rollback automático).
     *         7. Notificação ao recebedor é despachada assincronamente via RabbitMQ.
     *
     * [EN]    Executes money transfer between two users applying all business rules.
     *         Business Rules:
     *         1. Payer and payee must exist in the database.
     *         2. Payer cannot be a MERCHANT (merchants only receive).
     *         3. Payer cannot transfer to themselves.
     *         4. Payer must have sufficient balance (balance >= value).
     *         5. Transaction must be authorized by an external HTTP service.
     *         6. Debit and credit are executed within an ACID transaction (@Transactional with automatic rollback).
     *         7. Notification to the receiver is dispatched asynchronously via RabbitMQ.
     *
     * @param request Dados da requisição de transferência / Transfer request data (payer, payee, value)
     * @return Detalhes da transação persistida / Persisted transaction details
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

    /**
     * [PT-BR] Retorna o histórico de todas as transações realizadas no sistema.
     * [EN]    Retrieves the history of all transactions performed in the system.
     *
     * @return Lista de transações / List of transactions
     */
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
