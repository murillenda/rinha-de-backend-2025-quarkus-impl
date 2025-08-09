package br.com.rinha.murillenda.api.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(
        String status,
        @JsonProperty("minimum_response_time_ms")
        int minimumResponseTimeMs
) {}