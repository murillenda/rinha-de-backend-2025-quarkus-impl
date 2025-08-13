package br.com.rinha.murillenda.api.dto.input;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentInput(UUID correlationId, BigDecimal amount) {}
