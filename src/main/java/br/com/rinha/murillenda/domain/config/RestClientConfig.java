package br.com.rinha.murillenda.domain.config;

import br.com.rinha.murillenda.domain.external.PaymentProcessorClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class RestClientConfig {

    @Produces
    @ApplicationScoped
    @Named("defaultClient")
    public PaymentProcessorClient createDefaultClient(
            @ConfigProperty(name = "processor.default.url") String url) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .connectTimeout(250, TimeUnit.MILLISECONDS)
                .readTimeout(800, TimeUnit.MILLISECONDS)
                .build(PaymentProcessorClient.class);
    }

    @Produces
    @ApplicationScoped
    @Named("fallbackClient")
    public PaymentProcessorClient createFallbackClient(
            @ConfigProperty(name = "processor.fallback.url") String url) {
        return RestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .connectTimeout(250, TimeUnit.MILLISECONDS)
                .readTimeout(800, TimeUnit.MILLISECONDS)
                .build(PaymentProcessorClient.class);
    }

}
