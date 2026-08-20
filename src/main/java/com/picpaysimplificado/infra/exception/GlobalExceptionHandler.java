package com.picpaysimplificado.infra.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [PT-BR] Trata exceções de entidade não encontrada, retornando status HTTP 404 (Not Found).
     * [EN]    Handles entity not found exceptions, returning HTTP status 404 (Not Found).
     *
     * @param ex Exceção capturada / Captured exception
     * @return Resposta JSON com status 404 / JSON response with status 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * [PT-BR] Trata erro de saldo insuficiente durante transferências, retornando HTTP 422 (Unprocessable Entity).
     * [EN]    Handles insufficient balance errors during transfers, returning HTTP 422 (Unprocessable Entity).
     *
     * @param ex Exceção capturada / Captured exception
     * @return Resposta JSON com status 422 / JSON response with status 422
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /**
     * [PT-BR] Trata recusa do serviço autorizador externo, retornando HTTP 403 (Forbidden).
     * [EN]    Handles external authorizer refusal, returning HTTP 403 (Forbidden).
     *
     * @param ex Exceção capturada / Captured exception
     * @return Resposta JSON com status 403 / JSON response with status 403
     */
    @ExceptionHandler(UnauthorizedTransactionException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedTransaction(UnauthorizedTransactionException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * [PT-BR] Trata violações de regras de negócio (ex: lojista enviando dinheiro ou para si mesmo), retornando HTTP 403 (Forbidden).
     * [EN]    Handles business rule violations (e.g. merchant sending money or to oneself), returning HTTP 403 (Forbidden).
     *
     * @param ex Exceção capturada / Captured exception
     * @return Resposta JSON com status 403 / JSON response with status 403
     */
    @ExceptionHandler(TransactionNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionNotAllowed(TransactionNotAllowedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * [PT-BR] Trata erros de validação de DTOs (@Valid / Jakarta Validation), retornando lista de campos inválidos com HTTP 400.
     * [EN]    Handles DTO validation errors (@Valid / Jakarta Validation), returning list of invalid fields with HTTP 400.
     *
     * @param ex Exceção de validação / Validation exception
     * @return Resposta JSON estruturada com lista de campos e mensagens / Structured JSON with fields and error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> errorMap = new LinkedHashMap<>();
                    errorMap.put("field", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    return errorMap;
                })
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        body.put("details", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * [PT-BR] Trata argumentos ilegais (ex: documento ou email duplicado), retornando HTTP 400 (Bad Request).
     * [EN]    Handles illegal arguments (e.g. duplicate document or email), returning HTTP 400 (Bad Request).
     *
     * @param ex Exceção capturada / Captured exception
     * @return Resposta JSON com status 400 / JSON response with status 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * [PT-BR] Trata qualquer outra exceção não mapeada, retornando HTTP 500 (Internal Server Error).
     * [EN]    Handles any other unmapped exception, returning HTTP 500 (Internal Server Error).
     *
     * @param ex Exceção genérica / Generic exception
     * @return Resposta JSON com status 500 / JSON response with status 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor: " + ex.getMessage());
    }

    /**
     * [PT-BR] Construtor utilitário para padronizar o payload JSON de resposta de erro.
     * [EN]    Utility helper to standardize the error JSON response payload.
     *
     * @param status Status HTTP / HTTP status
     * @param message Mensagem explicativa do erro / Error explanation message
     * @return ResponseEntity contendo mapa com timestamp, status, erro e mensagem / ResponseEntity containing error map
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
