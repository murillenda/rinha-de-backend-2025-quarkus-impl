package br.com.rinha.murillenda.api;

import br.com.rinha.murillenda.domain.service.PaymentService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;

@Path("/")
public class PaymentsResource {

    @Inject
    PaymentService paymentService;

    @POST
    @Path("/payments")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> createPayment() {
        BigDecimal amountToPay = new BigDecimal("100.00");

        return paymentService.processNewPayment(amountToPay)
                .map(result -> {
                    if (result.success()) {
                        return Response.ok(result.message()).build();
                    } else {
                        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                .entity(result.message()).build();
                    }
                });
    }

    @GET
    @Path("/admin/payments-summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getPaymentsSummary() {
        return paymentService.getSummary()
                .map(summary -> Response.ok(summary).build());
    }
}