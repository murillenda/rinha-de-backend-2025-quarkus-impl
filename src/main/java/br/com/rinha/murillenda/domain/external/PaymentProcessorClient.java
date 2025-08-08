package br.com.rinha.murillenda.domain.external;

import br.com.rinha.murillenda.domain.dto.external.input.PaymentRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey="payment-processor-api")
@Path("/payments")
public interface PaymentProcessorClient {

    @GET
    @Path("/service-health")
    String getHealth();

    @POST
    void processPayment(PaymentRequest request);
}
