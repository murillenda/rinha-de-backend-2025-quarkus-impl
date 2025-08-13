package br.com.rinha.murillenda.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID correlationId,
        BigDecimal amount,
        Instant requested_at,
        String status,
        String processor
) {}