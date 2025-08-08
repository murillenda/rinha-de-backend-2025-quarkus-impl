package br.com.rinha.murillenda.domain.dto.external.output;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(
        String status,
        @JsonProperty("minimum_response_time_ms")
        int latency
) {}