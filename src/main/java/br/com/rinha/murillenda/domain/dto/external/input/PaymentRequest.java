package br.com.rinha.murillenda.domain.dto.external.input;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {}