package com.picpaysimplificado.service;

import com.picpaysimplificado.infra.exception.UnauthorizedTransactionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class AuthorizationService {

    private final RestClient restClient;
    private final String authorizationUrl;

    public AuthorizationService(
            RestClient.Builder restClientBuilder,
            @Value("${picpay.services.authorization-url}") String authorizationUrl) {
        this.restClient = restClientBuilder.build();
        this.authorizationUrl = authorizationUrl;
    }

    /**
     * Consults the external authorization service before completing a transfer.
     * Throws UnauthorizedTransactionException if authorization is denied.
     */
    public void authorize() {
        log.info("Consultando serviço autorizador externo: {}", authorizationUrl);

        try {
            var response = restClient.get()
                    .uri(authorizationUrl)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                var body = response.getBody();
                String status = (String) body.get("status");

                if ("success".equals(status)) {
                    var data = (Map<?, ?>) body.get("data");
                    if (data != null && Boolean.TRUE.equals(data.get("authorization"))) {
                        log.info("Transferência autorizada pelo serviço externo");
                        return;
                    }
                }
            }

            log.warn("Transferência negada pelo serviço autorizador externo");
            throw new UnauthorizedTransactionException("Transferência não autorizada pelo serviço externo");

        } catch (UnauthorizedTransactionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao consultar serviço autorizador: {}", e.getMessage());
            throw new UnauthorizedTransactionException(
                    "Não foi possível autorizar a transferência. Tente novamente mais tarde.");
        }
    }
}
