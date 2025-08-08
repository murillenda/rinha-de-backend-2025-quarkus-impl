package br.com.rinha.murillenda.domain.dto.external.input;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID correlationId, BigDecimal amount) {}