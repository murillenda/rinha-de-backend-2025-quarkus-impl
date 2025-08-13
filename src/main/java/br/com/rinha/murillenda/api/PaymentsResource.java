package br.com.rinha.murillenda.api;

import br.com.rinha.murillenda.api.dto.input.PaymentInput;
import br.com.rinha.murillenda.domain.model.Payment;
import br.com.rinha.murillenda.domain.repository.PaymentRepository;
import br.com.rinha.murillenda.domain.service.PaymentService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@Path("/")
public class PaymentsResource {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentService paymentService;

    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> createPayment(PaymentInput input) {
        var paymentJob = new Payment(input.correlationId(), input.amount(), Instant.now(), "PENDING", null);

        return paymentRepository.save(paymentJob)
                .map(v -> Response.accepted()
                        .entity("Payment accepted and queued for processing.")
                        .build());
    }

    @GET
    @Path("/payments-summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getPaymentsSummary(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {

        if (fromStr == null || toStr == null || fromStr.isBlank() || toStr.isBlank()) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Query params 'from' and 'to' are required.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }

        try {
            Instant fromInstant = Instant.parse(fromStr);
            Instant toInstant = Instant.parse(toStr);

            return paymentService.getSummary(fromInstant, toInstant)
                    .map(summary -> Response.ok(summary).build());
        } catch (java.time.format.DateTimeParseException e) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid date format for 'from' or 'to'.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build());
        }
    }
}