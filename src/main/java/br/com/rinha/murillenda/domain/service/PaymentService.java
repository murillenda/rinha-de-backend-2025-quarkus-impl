package br.com.rinha.murillenda.domain.service;

import br.com.rinha.murillenda.api.dto.output.PaymentSummaryResponse;
import br.com.rinha.murillenda.domain.dto.external.input.PaymentRequest;
import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import br.com.rinha.murillenda.domain.model.Payment;
import br.com.rinha.murillenda.domain.repository.PaymentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class PaymentService {

    @Inject
    @Named("defaultClient")
    PaymentProcessorClient defaultClient;

    @Inject
    @Named("fallbackClient")
    PaymentProcessorClient fallbackClient;

    @Inject
    ProcessorHealthService healthService;

    @Inject
    PaymentRepository paymentRepository;

    public Uni<PaymentResult> processNewPayment(BigDecimal amount) {
        UUID paymentId = UUID.randomUUID();
        var request = new PaymentRequest(paymentId, amount);

        return healthService.isDefaultHealthy()
                .flatMap(isHealthy -> {
                    if (isHealthy) {
                        log.info("SERVICE LOGIC: Default saudável, tentando pagamento...");
                        // --- LÓGICA DE RETRY REATIVO --- //
                        return defaultClient.processPayment(request)
                                .onFailure().retry().withBackOff(Duration.ofMillis(50), Duration.ofMillis(100)).atMost(3)
                                .map(v -> true); // Se sucesso, o resultado é 'true'
                    }
                    // Se não está saudável, consideramos uma falha para acionar o fallback
                    return Uni.createFrom().failure(new IllegalStateException("Default processor not healthy"));
                })
                .onFailure().recoverWithUni(failure -> {
                    // Se a tentativa no default falhou (após os retries), tenta o fallback
                    log.warn("SERVICE LOGIC: Falha no DEFAULT, tentando fallback... Causa: {}", failure.getMessage());
                    return tryFallback(request);
                })
                .flatMap(processedSuccessfully -> {
                    if (processedSuccessfully) {
                        return paymentRepository.save(new Payment(paymentId, amount, Instant.now()))
                                .map(v -> new PaymentResult(true, "Pagamento processado com sucesso!"));
                    }
                    return Uni.createFrom().item(new PaymentResult(false, "Ambos os processadores de pagamento estão indisponíveis."));
                });
    }

    private Uni<Boolean> tryFallback(PaymentRequest request) {
        return healthService.isFallbackHealthy()
                .flatMap(isHealthy -> {
                    if (isHealthy) {
                        log.info("SERVICE LOGIC: Fallback saudável, tentando pagamento...");
                        // --- LÓGICA DE RETRY REATIVO --- //
                        return fallbackClient.processPayment(request)
                                .onFailure().retry().withBackOff(Duration.ofMillis(50)).atMost(3)
                                .map(v -> true);
                    }
                    log.warn("SERVICE LOGIC: Fallback não está saudável para ser tentado.");
                    return Uni.createFrom().item(false);
                })
                .onFailure().recoverWithItem(failure -> {
                    log.error("SERVICE LOGIC: Fallback também falhou após retries!", failure);
                    return false;
                });
    }

    public Uni<PaymentSummaryResponse> getSummary() {
        return paymentRepository.getSummary();
    }

    public record PaymentResult(boolean success, String message) {}
}