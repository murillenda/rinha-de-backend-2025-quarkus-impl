package br.com.rinha.murillenda.domain.external;

import br.com.rinha.murillenda.api.dto.external.HealthResponse;
import br.com.rinha.murillenda.domain.dto.external.input.PaymentRequest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/payments")
public interface PaymentProcessorClient {

    @GET
    @Path("/service-health")
    Uni<HealthResponse> getHealth();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Uni<Void> processPayment(PaymentRequest request);
}
