package com.picpaysimplificado.domain.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * [PT-BR] Busca todas as transações enviadas por um usuário específico.
     * [EN]    Finds all transactions sent by a specific user.
     *
     * @param senderId ID do usuário remetente / Sender user ID
     * @return Lista de transações enviadas / List of sent transactions
     */
    List<Transaction> findBySenderId(Long senderId);

    /**
     * [PT-BR] Busca todas as transações recebidas por um usuário específico.
     * [EN]    Finds all transactions received by a specific user.
     *
     * @param receiverId ID do usuário recebedor / Receiver user ID
     * @return Lista de transações recebidas / List of received transactions
     */
    List<Transaction> findByReceiverId(Long receiverId);
}
