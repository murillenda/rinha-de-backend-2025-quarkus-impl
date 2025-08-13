package br.com.rinha.murillenda.api.dto.output;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record ProcessorSummary(long totalRequests, BigDecimal totalAmount) {
    public ProcessorSummary() { this(0, BigDecimal.ZERO); }
}