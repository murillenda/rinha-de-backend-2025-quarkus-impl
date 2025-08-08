package br.com.rinha.murillenda.domain.service;

import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import br.com.rinha.murillenda.domain.model.ProcessorStatus;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@ApplicationScoped
public class ProcessorHealthService {
    private static final String DEFAULT = "default";
    private static final String FALLBACK = "fallback";

    @Inject
    @Named("defaultClient")
    PaymentProcessorClient defaultClient;

    @Inject
    @Named("fallbackClient")
    PaymentProcessorClient fallbackClient;

    @ConfigProperty(name = "healthcheck.scheduler.enabled", defaultValue = "true")
    boolean schedulerEnabled;

    public ProcessorHealthService() {}

    @Scheduled(cron = "*/5 * * * * ?")
    @Transactional
    void checkProcessorsHealth() {
        if (!schedulerEnabled) {
            return;
        }

        log.info("Líder em ação: Verificando saúde dos processadores...");
        checkAndSaveStatus(DEFAULT, defaultClient);
        checkAndSaveStatus(FALLBACK, fallbackClient);
    }

    private void checkAndSaveStatus(String processorName, PaymentProcessorClient client) {
        ProcessorStatus status = ProcessorStatus.findById(processorName);

        // --- CIRCUIT BREAKER ---
        if (status != null && !status.isHealthy
                && Duration.between(status.lastChecked, Instant.now()).getSeconds() < 15) {
            log.info("Processador {} em 'castigo' (Circuit Breaker aberto). Pulando verificação.", processorName);
            return; // Pula a chamada de rede
        }
        // ---------------------------------

        try {
            client.getHealth();
            updateStatus(processorName, true);
        } catch (Exception e) {
            updateStatus(processorName, false);
            log.warn("Processador {} está INSTÁVEL: {}. Abrindo o circuito.", processorName, e.getMessage());
        }
    }

    private void updateStatus(String processorName, boolean isHealthy) {
        ProcessorStatus status = ProcessorStatus.findById(processorName);

        if (status == null) {
            status = new ProcessorStatus();
            status.processorName = processorName;
        }

        status.isHealthy = isHealthy;
        status.lastChecked = Instant.now();
        status.persist();
    }

    public boolean isDefaultHealthy() {
        return isProcessorHealthy(DEFAULT);
    }

    public boolean isFallbackHealthy() {
        return isProcessorHealthy(FALLBACK);
    }

    private boolean isProcessorHealthy(String processorName) {
        ProcessorStatus status = ProcessorStatus.findById(processorName);

        if (status == null) {
            log.warn("Status para {} é nulo. Considerado instável.", processorName);
            return false;
        }

        Instant now = Instant.now();
        Instant lastChecked = status.lastChecked;
        long secondsSinceLastCheck = Duration.between(lastChecked, now).getSeconds();

        log.info("DEBUG: Verificando {} | DB lastChecked: {} | App now: {} | Diferença (s): {}",
                processorName, lastChecked, now.toString(), secondsSinceLastCheck);

        if (secondsSinceLastCheck > 30) {
            log.warn("Status para {} é muito antigo ({} > 30). Considerado instável.", processorName, secondsSinceLastCheck);
            return false;
        }

        if (!status.isHealthy) {
            log.warn("Status para {} no DB é 'isHealthy=false'. Considerado instável.", processorName);
            return false;
        }

        return true;
    }
}
