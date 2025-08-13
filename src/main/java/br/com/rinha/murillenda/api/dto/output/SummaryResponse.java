package br.com.rinha.murillenda.api.dto.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SummaryResponse(
        @JsonProperty("default") ProcessorSummary defaultSummary,
        @JsonProperty("fallback") ProcessorSummary fallbackSummary
) {
    public SummaryResponse() { this(null, null); }
}