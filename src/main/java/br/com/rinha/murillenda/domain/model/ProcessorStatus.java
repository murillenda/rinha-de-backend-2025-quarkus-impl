package br.com.rinha.murillenda.domain.model;

import java.time.Instant;

public record ProcessorStatus(
        String processorName,
        boolean isHealthy,
        Instant lastChecked
) {}