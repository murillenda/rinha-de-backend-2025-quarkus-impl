package br.com.rinha.murillenda.domain.service;

import br.com.rinha.murillenda.domain.dto.external.input.PaymentRequest;
import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import br.com.rinha.murillenda.domain.model.Payment;
import br.com.rinha.murillenda.domain.repository.PaymentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
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

    public PaymentService() {}

    /**
     * Orquestra o processamento de um novo pagamento.
     */
    @Transactional
    public PaymentResult processNewPayment(BigDecimal amount) {
        UUID paymentId = UUID.randomUUID();
        var request = new PaymentRequest(paymentId, amount);
        boolean processedSuccessfully = false;

        if (healthService.isDefaultHealthy()) {
            log.info("SERVICE LOGIC: Tentando processador DEFAULT.");
            try {
                defaultClient.processPayment(request);
                processedSuccessfully = true;
            } catch (Exception e) {
                log.error("SERVICE LOGIC: Falha no DEFAULT. Tentando fallback...");
                processedSuccessfully = tryFallback(request);
            }
        } else {
            log.info("SERVICE LOGIC: Default instável. Tentando processador FALLBACK.");
            processedSuccessfully = tryFallback(request);
        }

        if (processedSuccessfully) {
            persistPayment(paymentId, amount);
            return new PaymentResult(true, "Pagamento processado com sucesso!");
        } else {
            return new PaymentResult(false, "Ambos os processadores de pagamento estão indisponíveis.");
        }
    }

    private boolean tryFallback(PaymentRequest request) {
        if (healthService.isFallbackHealthy()) {
            try {
                fallbackClient.processPayment(request);
                log.info("SERVICE LOGIC: Sucesso no FALLBACK.");
                return true;
            } catch (Exception e) {
                log.error("SERVICE LOGIC: Fallback também falhou!");
                return false;
            }
        }
        log.error("SERVICE LOGIC: Fallback não está saudável para ser tentado.");
        return false;
    }

    private void persistPayment(UUID correlationId, BigDecimal amount) {
        Payment p = new Payment();
        p.correlationId = correlationId;
        p.amount = amount;
        p.requested_at = Instant.now();
        paymentRepository.persist(p);
        log.info("SERVICE LOGIC: Pagamento persistido no banco de dados.");
    }

    public record PaymentResult(boolean success, String message) {}

}
