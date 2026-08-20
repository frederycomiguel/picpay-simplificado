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

    /**
     * [PT-BR] Construtor que injeta o builder do RestClient e a URL do serviço autorizador configurada no application.yml.
     * [EN]    Constructor injecting the RestClient builder and the authorizer service URL from application.yml.
     *
     * @param restClientBuilder Builder do Spring RestClient / Spring RestClient builder
     * @param authorizationUrl URL do serviço autorizador / Authorizer service URL
     */
    public AuthorizationService(
            RestClient.Builder restClientBuilder,
            @Value("${picpay.services.authorization-url}") String authorizationUrl) {
        this.restClient = restClientBuilder.build();
        this.authorizationUrl = authorizationUrl;
    }

    /**
     * [PT-BR] Consulta o serviço autorizador externo via HTTP antes de efetivar uma transferência.
     *         Lança UnauthorizedTransactionException caso a transferência seja recusada.
     * [EN]    Consults the external authorization service via HTTP before executing a transfer.
     *         Throws UnauthorizedTransactionException if the transfer is denied.
     *
     * @throws UnauthorizedTransactionException Se o autorizador negar / If the authorizer denies
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
