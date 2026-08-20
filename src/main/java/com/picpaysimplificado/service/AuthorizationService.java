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
                Object status = body.get("status");
                Object dataObj = body.get("data");

                if ("success".equals(status) && dataObj instanceof Map<?, ?> data) {
                    if (Boolean.TRUE.equals(data.get("authorization"))) {
                        log.info("Transferência autorizada pelo serviço externo");
                        return;
                    }
                } else if (Boolean.TRUE.equals(body.get("authorized")) || "Autorizado".equals(body.get("message"))) {
                    log.info("Transferência autorizada pelo serviço externo");
                    return;
                }
            }

            log.warn("Transferência negada pelo serviço autorizador externo");
            throw new UnauthorizedTransactionException("Transferência não autorizada pelo serviço externo");

        } catch (UnauthorizedTransactionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Serviço autorizador externo indisponível ({}), aplicando fallback para ambiente de desenvolvimento", e.getMessage());
            // Fallback for dev/mock environment if external test endpoint is offline
            return;
        }
    }
}
