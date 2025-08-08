package br.com.rinha.murillenda.api.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
        @JsonProperty("total_transactions")
        long totalTransactions,
        BigDecimal total_amount
) {}
