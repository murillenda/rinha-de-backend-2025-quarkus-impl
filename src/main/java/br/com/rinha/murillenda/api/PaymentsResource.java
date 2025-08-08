package br.com.rinha.murillenda.api;

import br.com.rinha.murillenda.domain.service.PaymentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Path("/")
public class PaymentsResource {

    @Inject
    PaymentService paymentService;

    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @Path("/payments")
    public Response createPayment() {
        BigDecimal amountToPay = new BigDecimal("100.00"); // Valor simulado

        var result = paymentService.processNewPayment(amountToPay);

        if (result.success()) {
            return Response.ok(result.message()).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(result.message())
                    .build();
        }
    }

    @GET
    @Path("/payments-summary")
    public Response getPaymentsSummary() {
        var summary = paymentService.getSummary();
        return Response.ok(summary).build();
    }
}
