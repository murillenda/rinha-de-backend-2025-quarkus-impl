package br.com.rinha.murillenda.domain.service;

import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ApplicationScoped
public class ProcessorHealthService {

    private final AtomicBoolean isDefaultProcessorHealthy = new AtomicBoolean(true);
    private final AtomicBoolean isFallbackProcessorHealthy = new AtomicBoolean(true);

    private final PaymentProcessorClient defaultClient;
    private final PaymentProcessorClient fallbackClient;

    public ProcessorHealthService(@ConfigProperty(name = "processor.default.url") String defaultUrl,
                                  @ConfigProperty(name = "processor.fallback.url") String fallbackUrl) {

        this.defaultClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(defaultUrl))
                .build(PaymentProcessorClient.class);

        this.fallbackClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(fallbackUrl))
                .build(PaymentProcessorClient.class);
    }

    @Scheduled(cron = "*/5 * * * * ?")
    void checkProcessorHealth() {

        log.info("Spy funcionando: Verificando saúde dos processadores...");

        try {
            defaultClient.getHealth();
            isDefaultProcessorHealthy.set(true);
            log.info("Processador Default está SAUDÁVEL.");

        } catch (Exception e) {
            isDefaultProcessorHealthy.set(false);
            log.warn("Processador Default está INSTÁVEL: {}", e.getMessage());
        }

        try {
            fallbackClient.getHealth();
            isFallbackProcessorHealthy.set(true);
            log.info("Processador Fallback está SAUDÁVEL.");

        } catch (Exception e) {
            isDefaultProcessorHealthy.set(false);
            log.warn("Processador Fallback está INSTÁVEL: {}", e.getMessage());
        }
    }

    public boolean isDefaultHealthy() {
        return isDefaultProcessorHealthy.get();
    }

    public boolean isFallbackHealthy() {
        return isFallbackProcessorHealthy.get();
    }

}
