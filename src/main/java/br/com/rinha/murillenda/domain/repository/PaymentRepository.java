package br.com.rinha.murillenda.domain.repository;

import br.com.rinha.murillenda.api.dto.output.ProcessorSummary;
import br.com.rinha.murillenda.api.dto.output.SummaryResponse;
import br.com.rinha.murillenda.domain.model.Payment;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PaymentRepository {

    @Inject
    PgPool client;

    public Uni<Void> save(Payment payment) {
        return client.withTransaction(conn ->
                conn.preparedQuery("INSERT INTO payments (correlationId, amount, requested_at, status, processor) VALUES ($1, $2, $3, $4, $5)")
                        .execute(Tuple.of(
                                payment.correlationId(),
                                payment.amount(),
                                LocalDateTime.ofInstant(payment.requested_at(), ZoneOffset.UTC),
                                payment.status(),
                                payment.processor()
                        ))
        ).replaceWithVoid();
    }

    public Uni<List<Payment>> findAndLockPending(int limit) {
        return client.withTransaction(conn ->
                conn.preparedQuery("SELECT * FROM payments WHERE status = 'PENDING' ORDER BY requested_at LIMIT $1 FOR UPDATE SKIP LOCKED")
                        .execute(Tuple.of(limit))
                        .map(this::mapRowsToPaymentList)
        );
    }

    public Uni<Void> updateStatus(UUID correlationId, String status) {
        return client.withTransaction(conn ->
                conn.preparedQuery("UPDATE payments SET status = $1 WHERE correlationId = $2")
                        .execute(Tuple.of(status, correlationId))
        ).replaceWithVoid();
    }

    private List<Payment> mapRowsToPaymentList(RowSet<Row> rows) {
        var payments = new ArrayList<Payment>();
        for (Row row : rows) {
            String amountAsString = row.getString("amount");
            BigDecimal amount = (amountAsString == null) ? BigDecimal.ZERO : new BigDecimal(amountAsString);

            payments.add(new Payment(
                    row.getUUID("correlationid"),
                    amount,
                    row.getLocalDateTime("requested_at").toInstant(java.time.ZoneOffset.UTC),
                    row.getString("status"),
                    row.getString("processor")
            ));
        }
        return payments;
    }

    public Uni<SummaryResponse> getSummary(Instant from, Instant to) {
        String sql = "SELECT processor, COUNT(*) as total_requests, SUM(amount) as total_amount " +
                "FROM payments " +
                "WHERE status = 'PROCESSED' AND requested_at BETWEEN $1 AND $2 " +
                "GROUP BY processor";

        Tuple params = Tuple.of(LocalDateTime.ofInstant(from, ZoneOffset.UTC), LocalDateTime.ofInstant(to, ZoneOffset.UTC));

        return client.preparedQuery(sql)
                .execute(params)
                .map(rows -> {
                    ProcessorSummary defaultSummary = new ProcessorSummary(0, BigDecimal.ZERO);
                    ProcessorSummary fallbackSummary = new ProcessorSummary(0, BigDecimal.ZERO);

                    for (Row row : rows) {
                        String processor = row.getString("processor");
                        long count = row.getLong("total_requests");

                        String sumAsString = row.getString("total_amount");
                        BigDecimal sum = (sumAsString == null) ? BigDecimal.ZERO : new BigDecimal(sumAsString);

                        if ("default".equals(processor)) {
                            defaultSummary = new ProcessorSummary(count, sum);
                        } else if ("fallback".equals(processor)) {
                            fallbackSummary = new ProcessorSummary(count, sum);
                        }
                    }
                    return new SummaryResponse(defaultSummary, fallbackSummary);
                });
    }

    public Uni<Void> updateStatusAndProcessor(UUID correlationId, String status, String processor) {
        return client.withTransaction(conn ->
                conn.preparedQuery("UPDATE payments SET status = $1, processor = $2 WHERE correlationId = $3")
                        .execute(Tuple.of(status, processor, correlationId))
        ).replaceWithVoid();
    }
}