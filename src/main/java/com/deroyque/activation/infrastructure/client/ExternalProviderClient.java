package com.deroyque.activation.infrastructure.client;

import com.deroyque.activation.application.port.out.ExternalProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ExternalProviderClient implements ExternalProviderPort {

    private static final Logger log = LoggerFactory.getLogger(ExternalProviderClient.class);

    private final RestClient restClient;

    public ExternalProviderClient(RestClient externalProviderRestClient) {
        this.restClient = externalProviderRestClient;
    }

    @Override
    public ActivationResult activate(String idempotencyKey, String customerId, String productCode) {
        log.info("Sending activation request to external provider for customer {} with idempotencyKey {}",
                customerId, idempotencyKey);

        Map<String, String> body = Map.of(
                "customerId", customerId,
                "productCode", productCode
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/provider/v1/activate")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        byte[] bytes = res.getBody().readAllBytes();
                        throw HttpClientErrorException.create(res.getStatusCode(), res.getStatusText(), res.getHeaders(), bytes, StandardCharsets.UTF_8);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        byte[] bytes = res.getBody().readAllBytes();
                        throw HttpServerErrorException.create(res.getStatusCode(), res.getStatusText(), res.getHeaders(), bytes, StandardCharsets.UTF_8);
                    })
                    .body(Map.class);

            String code = response != null && response.containsKey("activationCode")
                    ? String.valueOf(response.get("activationCode"))
                    : "OK";

            log.info("External provider successfully activated product with code {}", code);
            return new ActivationResult.Success(code);

        } catch (HttpClientErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("External provider returned client error {}: {}", ex.getStatusCode().value(), responseBody);
            return new ActivationResult.ClientError(ex.getStatusCode().value(), responseBody);

        } catch (HttpServerErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.warn("External provider returned server error {}: {}", ex.getStatusCode().value(), responseBody);
            return new ActivationResult.ServerError(ex.getStatusCode().value(), responseBody);

        } catch (ResourceAccessException ex) {
            log.warn("External provider timeout or connection failure: {}", ex.getMessage());
            return new ActivationResult.Timeout(ex.getMessage());

        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof SocketTimeoutException || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout"))) {
                log.warn("External provider timeout identified: {}", ex.getMessage());
                return new ActivationResult.Timeout(ex.getMessage());
            }
            log.error("Unexpected error during external provider call", ex);
            return new ActivationResult.ServerError(500, ex.getMessage());
        }
    }
}
