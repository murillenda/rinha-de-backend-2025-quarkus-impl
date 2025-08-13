package br.com.rinha.murillenda.domain.service;

import br.com.rinha.murillenda.api.dto.output.PaymentSummaryResponse;
import br.com.rinha.murillenda.api.dto.output.SummaryResponse;
import br.com.rinha.murillenda.domain.model.ProcessingResult;
import br.com.rinha.murillenda.domain.dto.external.input.PaymentRequest;
import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import br.com.rinha.murillenda.domain.model.Payment;
import br.com.rinha.murillenda.domain.repository.PaymentRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

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

    public Uni<ProcessingResult> processQueuedPayment(Payment paymentJob) {
        var request = new PaymentRequest(paymentJob.correlationId(), paymentJob.amount());

        // Tenta o fluxo do 'default'
        Uni<ProcessingResult> defaultFlow = healthService.isDefaultHealthy()
                .flatMap(isHealthy -> {
                    if (!isHealthy) {
                        return Uni.createFrom().failure(new IllegalStateException("Default processor not healthy"));
                    }
                    return defaultClient.processPayment(request)
                            .onFailure().retry().withBackOff(Duration.ofMillis(50), Duration.ofMillis(100)).atMost(3)
                            .map(v -> new ProcessingResult(true, "default")); // <-- Retorna sucesso com o nome do processador
                });

        // O fallback agora retorna o resultado com seu nome
        return defaultFlow.onFailure().recoverWithUni(() -> {
                    log.warn("SERVICE LOGIC: Falha no fluxo DEFAULT, tentando fallback...");
                    return healthService.isFallbackHealthy()
                            .flatMap(isHealthy -> {
                                if (!isHealthy) {
                                    return Uni.createFrom().failure(new IllegalStateException("Fallback processor not healthy"));
                                }
                                return fallbackClient.processPayment(request)
                                        .onFailure().retry().withBackOff(Duration.ofMillis(50)).atMost(3)
                                        .map(v -> new ProcessingResult(true, "fallback")); // <-- Retorna sucesso com o nome do processador
                            });
                })
                // Se ambos falharem, o resultado final é 'falha' sem nome de processador
                .onFailure().recoverWithItem(new ProcessingResult(false, null));
    }

    // O método de resumo continua igual
    public Uni<SummaryResponse> getSummary(Instant from, Instant to) {
        return paymentRepository.getSummary(from, to);
    }
}