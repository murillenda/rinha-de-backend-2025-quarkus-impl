package br.com.rinha.murillenda.domain.service;

import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import br.com.rinha.murillenda.domain.model.ProcessorStatus;
import br.com.rinha.murillenda.domain.repository.ProcessorStatusRepository;
import io.quarkus.cache.CacheResult;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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

    @Inject
    ProcessorStatusRepository statusRepository;

    @ConfigProperty(name = "healthcheck.scheduler.enabled", defaultValue = "true")
    boolean schedulerEnabled;

    @Scheduled(cron = "*/5 * * * * ?")
    void checkProcessorsHealth() {
        if (!schedulerEnabled) {
            return;
        }

        log.info("Líder em ação: Verificando saúde dos processadores...");

        checkAndSaveStatus(DEFAULT, defaultClient)
                .subscribe().with(v -> log.info("Check do Default concluído."), err -> log.error("Falha no check do Default", err));

        checkAndSaveStatus(FALLBACK, fallbackClient)
                .subscribe().with(v -> log.info("Check do Fallback concluído."), err -> log.error("Falha no check do Fallback", err));
    }


    private Uni<Void> checkAndSaveStatus(String processorName, PaymentProcessorClient client) {
        return statusRepository.findById(processorName)
                .flatMap(statusOpt -> {
                    ProcessorStatus status = statusOpt.orElse(null);

                    if (status != null && !status.isHealthy() && Duration.between(status.lastChecked(), Instant.now()).getSeconds() < 15) {
                        log.info("Processador {} em 'castigo'. Pulando verificação.", processorName);
                        return Uni.createFrom().voidItem();
                    }

                    return client.getHealth() // Isso retorna Uni<HealthResponse>
                            .onItem().transformToUni(response -> {
                                log.info("Processador {} está SAUDÁVEL. Circuito fechado.", processorName);
                                return statusRepository.update(new ProcessorStatus(processorName, true, Instant.now()));
                            })
                            .onFailure().recoverWithUni(failure -> {
                                log.warn("Processador {} está INSTÁVEL. Abrindo o circuito.", processorName);
                                return statusRepository.update(new ProcessorStatus(processorName, false, Instant.now()));
                            });
                });
    }

    public Uni<Boolean> isDefaultHealthy() {
        return isProcessorHealthy(DEFAULT);
    }

    public Uni<Boolean> isFallbackHealthy() {
        return isProcessorHealthy(FALLBACK);
    }

    @CacheResult(cacheName = "health-status-cache")
    public Uni<Boolean> isProcessorHealthy(String processorName) {
        return statusRepository.findById(processorName)
                .map(statusOpt -> {
                    if (statusOpt.isEmpty()) {
                        log.warn("Status para {} é nulo. Considerado instável.", processorName);
                        return false;
                    }

                    ProcessorStatus status = statusOpt.get();
                    long secondsSinceLastCheck = Duration.between(status.lastChecked(), Instant.now()).getSeconds();

                    log.info("DEBUG: Verificando {} | DB lastChecked: {} | Diferença (s): {}", processorName,
                            status.lastChecked(), secondsSinceLastCheck);

                    if (secondsSinceLastCheck > 30) {
                        log.warn("Status para {} é muito antigo ({} > 30). Considerado instável.", processorName, secondsSinceLastCheck);
                        return false;
                    }

                    return status.isHealthy();
                });
    }
}