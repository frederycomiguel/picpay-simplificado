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
import com.picpaysimplificado.infra.exception.UnauthorizedTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * [PT-BR] Suite de Testes Unitários para a classe {@link TransactionService}.
 *         Valida todas as regras de negócio core do desafio PicPay Simplificado:
 *         - Transferência bem-sucedida entre usuário comum e lojista com débito/crédito atômico.
 *         - Bloqueio de transferências originadas por Lojistas (apenas recebem).
 *         - Bloqueio de transferências para si mesmo (pagador == recebedor).
 *         - Validação de saldo insuficiente antes de qualquer operação de escrita.
 *         - Integração com serviço autorizador externo via HTTP.
 *         - Resiliência de mensageria RabbitMQ (falha no broker não deve cancelar a transferência).
 *
 * [EN]    Unit Test Suite for {@link TransactionService}.
 *         Validates all core business rules of the PicPay Simplificado challenge:
 *         - Successful transfer between common user and merchant with atomic debit/credit.
 *         - Merchant transfer restriction (merchants only receive).
 *         - Self-transfer restriction (payer == payee).
 *         - Insufficient balance validation before state changes.
 *         - External HTTP authorization integration.
 *         - RabbitMQ messaging resilience (broker failure must not roll back completed transfer).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User commonPayer;
    private User merchantPayee;
    private User commonPayee;

    @BeforeEach
    void setUp() {
        commonPayer = User.builder()
                .id(1L)
                .firstName("João")
                .lastName("Silva")
                .document("12345678901")
                .email("joao@email.com")
                .password("senha123")
                .balance(new BigDecimal("1000.00"))
                .userType(UserType.COMMON)
                .build();

        merchantPayee = User.builder()
                .id(2L)
                .firstName("Loja")
                .lastName("ABC")
                .document("12345678000190")
                .email("loja@email.com")
                .password("senha123")
                .balance(new BigDecimal("500.00"))
                .userType(UserType.MERCHANT)
                .build();

        commonPayee = User.builder()
                .id(3L)
                .firstName("Maria")
                .lastName("Oliveira")
                .document("98765432100")
                .email("maria@email.com")
                .password("senha123")
                .balance(new BigDecimal("200.00"))
                .userType(UserType.COMMON)
                .build();
    }

    @Nested
    @DisplayName("Transfer Execution Tests")
    class ExecuteTransferTests {

        /**
         * [PT-BR] [Cenário de Sucesso / Happy Path]
         *         Regra: Usuário COMUM com saldo suficiente transfere para LOJISTA.
         *         Verificações:
         *         1. Saldo do pagador é debitado (1000.00 -> 900.00).
         *         2. Saldo do recebedor é creditado (500.00 -> 600.00).
         *         3. Autorizador externo é consultado.
         *         4. Transação é persistida como COMPLETED.
         *         5. Notificação assíncrona é publicada no RabbitMQ.
         *
         * [EN]    [Happy Path Scenario]
         *         Rule: COMMON user with sufficient balance transfers to MERCHANT.
         *         Verifications:
         *         1. Payer balance is debited (1000.00 -> 900.00).
         *         2. Payee balance is credited (500.00 -> 600.00).
         *         3. External authorizer is called.
         *         4. Transaction is saved with status COMPLETED.
         *         5. Async notification is published to RabbitMQ.
         */
        @Test
        @DisplayName("Should successfully execute transfer from COMMON user to MERCHANT")
        void shouldExecuteTransferSuccessfully() {
            // Given: Usuário comum com R$ 1.000,00 transferindo R$ 100,00 para Lojista
            UUID txId = UUID.randomUUID();
            TransferRequest request = new TransferRequest(new BigDecimal("100.00"), 1L, 2L);

            when(userService.findById(1L)).thenReturn(commonPayer);
            when(userService.findById(2L)).thenReturn(merchantPayee);
            doNothing().when(authorizationService).authorize();

            Transaction savedTx = Transaction.builder()
                    .id(txId)
                    .amount(request.value())
                    .sender(commonPayer)
                    .receiver(merchantPayee)
                    .status(TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

            // When: Executa a operação de transferência
            TransferResponse response = transactionService.executeTransfer(request);

            // Then: Valida resposta e atualização dos saldos das partes envolvidas
            assertThat(response).isNotNull();
            assertThat(response.transactionId()).isEqualTo(txId);
            assertThat(response.amount()).isEqualByComparingTo("100.00");
            assertThat(response.payerId()).isEqualTo(1L);
            assertThat(response.payeeId()).isEqualTo(2L);
            assertThat(response.status()).isEqualTo("COMPLETED");

            // Valida débito do pagador e crédito do recebedor
            assertThat(commonPayer.getBalance()).isEqualByComparingTo("900.00");
            assertThat(merchantPayee.getBalance()).isEqualByComparingTo("600.00");

            verify(userService).save(commonPayer);
            verify(userService).save(merchantPayee);
            verify(authorizationService).authorize();
            verify(transactionRepository).save(any(Transaction.class));
            verify(notificationService).sendNotification(any(NotificationPayload.class));
        }

        /**
         * [PT-BR] Regra de Negócio: Lojistas só podem receber transferências, nunca enviar.
         *         Deve lançar TransactionNotAllowedException sem alterar saldos nem consultar autorizador.
         *
         * [EN]    Business Rule: Merchants can only receive money, never send.
         *         Must throw TransactionNotAllowedException without modifying balances or calling authorizer.
         */
        @Test
        @DisplayName("Should throw TransactionNotAllowedException when payer is a MERCHANT")
        void shouldThrowExceptionWhenPayerIsMerchant() {
            // Given: Pagador do tipo MERCHANT tentando enviar dinheiro
            User merchantPayer = User.builder()
                    .id(2L)
                    .firstName("Loja")
                    .lastName("ABC")
                    .userType(UserType.MERCHANT)
                    .balance(new BigDecimal("1000.00"))
                    .build();

            TransferRequest request = new TransferRequest(new BigDecimal("100.00"), 2L, 1L);

            when(userService.findById(2L)).thenReturn(merchantPayer);
            when(userService.findById(1L)).thenReturn(commonPayer);

            // When & Then: Deve lançar TransactionNotAllowedException
            assertThatThrownBy(() -> transactionService.executeTransfer(request))
                    .isInstanceOf(TransactionNotAllowedException.class)
                    .hasMessageContaining("Lojistas não podem realizar transferências");

            // Garante que nenhuma operação subsequente foi chamada
            verify(authorizationService, never()).authorize();
            verify(transactionRepository, never()).save(any());
            verify(notificationService, never()).sendNotification(any());
        }

        /**
         * [PT-BR] Regra de Negócio: Pagador não pode transferir dinheiro para si mesmo (payer.id == payee.id).
         *         Deve lançar TransactionNotAllowedException.
         *
         * [EN]    Business Rule: Payer cannot transfer money to themselves (payer.id == payee.id).
         *         Must throw TransactionNotAllowedException.
         */
        @Test
        @DisplayName("Should throw TransactionNotAllowedException when payer transfers to self")
        void shouldThrowExceptionWhenPayerEqualsPayee() {
            // Given: Requisição com mesmo ID para pagador e recebedor
            TransferRequest request = new TransferRequest(new BigDecimal("100.00"), 1L, 1L);

            when(userService.findById(1L)).thenReturn(commonPayer);

            // When & Then: Deve barrar a transferência para si mesmo
            assertThatThrownBy(() -> transactionService.executeTransfer(request))
                    .isInstanceOf(TransactionNotAllowedException.class)
                    .hasMessageContaining("Não é possível transferir dinheiro para si mesmo");

            verify(authorizationService, never()).authorize();
            verify(transactionRepository, never()).save(any());
        }

        /**
         * [PT-BR] Regra de Negócio: Pagador deve ter saldo suficiente (balance >= value).
         *         Caso contrário, lança InsufficientBalanceException sem consultar o autorizador.
         *
         * [EN]    Business Rule: Payer must have sufficient balance (balance >= value).
         *         Otherwise, throws InsufficientBalanceException without calling external authorizer.
         */
        @Test
        @DisplayName("Should throw InsufficientBalanceException when payer does not have enough balance")
        void shouldThrowExceptionWhenInsufficientBalance() {
            // Given: Pagador com R$ 1.000,00 tentando transferir R$ 2.000,00
            TransferRequest request = new TransferRequest(new BigDecimal("2000.00"), 1L, 2L);

            when(userService.findById(1L)).thenReturn(commonPayer);
            when(userService.findById(2L)).thenReturn(merchantPayee);

            // When & Then: Deve lançar InsufficientBalanceException
            assertThatThrownBy(() -> transactionService.executeTransfer(request))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Saldo insuficiente");

            verify(authorizationService, never()).authorize();
            verify(transactionRepository, never()).save(any());
        }

        /**
         * [PT-BR] Regra de Negócio: Autorização externa obrigatória antes de completar a transferência.
         *         Se o autorizador recusar, lança UnauthorizedTransactionException e reverte tudo.
         *
         * [EN]    Business Rule: External authorization required before executing transfer.
         *         If authorizer denies, throws UnauthorizedTransactionException and rolls back state.
         */
        @Test
        @DisplayName("Should throw UnauthorizedTransactionException when authorizer denies transfer")
        void shouldThrowExceptionWhenUnauthorized() {
            // Given: Autorizador externo simulando resposta negativa
            TransferRequest request = new TransferRequest(new BigDecimal("100.00"), 1L, 2L);

            when(userService.findById(1L)).thenReturn(commonPayer);
            when(userService.findById(2L)).thenReturn(merchantPayee);
            doThrow(new UnauthorizedTransactionException("Transferência não autorizada pelo serviço externo"))
                    .when(authorizationService).authorize();

            // When & Then: Deve lançar UnauthorizedTransactionException
            assertThatThrownBy(() -> transactionService.executeTransfer(request))
                    .isInstanceOf(UnauthorizedTransactionException.class);

            // Nenhum usuário deve ter o saldo salvo nem a transação persistida
            verify(userService, never()).save(any());
            verify(transactionRepository, never()).save(any());
            verify(notificationService, never()).sendNotification(any());
        }

        /**
         * [PT-BR] Regra de Negócio: Desacoplamento e Resiliência da Mensageria RabbitMQ.
         *         Se houver falha ao publicar a notificação na fila, a transferência bancária
         *         já consolidada NÃO deve sofrer rollback.
         *
         * [EN]    Business Rule: RabbitMQ Messaging Decoupling and Resilience.
         *         If publishing notification to the queue fails, the completed transfer
         *         MUST NOT be rolled back.
         */
        @Test
        @DisplayName("Should succeed even if async notification dispatch fails")
        void shouldSucceedEvenWhenNotificationFails() {
            // Given: Broker RabbitMQ indisponível simulando RuntimeException
            UUID txId = UUID.randomUUID();
            TransferRequest request = new TransferRequest(new BigDecimal("100.00"), 1L, 2L);

            when(userService.findById(1L)).thenReturn(commonPayer);
            when(userService.findById(2L)).thenReturn(merchantPayee);
            doNothing().when(authorizationService).authorize();

            Transaction savedTx = Transaction.builder()
                    .id(txId)
                    .amount(request.value())
                    .sender(commonPayer)
                    .receiver(merchantPayee)
                    .status(TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);
            doThrow(new RuntimeException("RabbitMQ connection down"))
                    .when(notificationService).sendNotification(any());

            // When: Executa a transferência
            TransferResponse response = transactionService.executeTransfer(request);

            // Then: Transação é salva com sucesso apesar da falha na notificação
            assertThat(response).isNotNull();
            assertThat(response.transactionId()).isEqualTo(txId);
            verify(transactionRepository).save(any(Transaction.class));
            verify(notificationService).sendNotification(any());
        }
    }

    @Nested
    @DisplayName("Transaction History Tests")
    class GetAllTransactionsTests {

        /**
         * [PT-BR] Histórico de Transações: Valida listagem de auditoria e conversão para DTOs.
         * [EN]    Transaction History: Validates audit listing and DTO mapping.
         */
        @Test
        @DisplayName("Should return list of all transactions")
        void shouldReturnAllTransactions() {
            // Given: 1 transação no repositório
            UUID txId = UUID.randomUUID();
            Transaction tx1 = Transaction.builder()
                    .id(txId)
                    .amount(new BigDecimal("50.00"))
                    .sender(commonPayer)
                    .receiver(merchantPayee)
                    .status(TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.findAll()).thenReturn(List.of(tx1));

            // When: Busca o histórico
            List<TransferResponse> result = transactionService.getAllTransactions();

            // Then: Valida retorno
            assertThat(result).hasSize(1);
            assertThat(result.get(0).transactionId()).isEqualTo(txId);
            assertThat(result.get(0).amount()).isEqualByComparingTo("50.00");
        }
    }
}
