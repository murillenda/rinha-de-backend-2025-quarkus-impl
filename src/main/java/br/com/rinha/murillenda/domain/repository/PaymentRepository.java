package br.com.rinha.murillenda.domain.repository;

import br.com.rinha.murillenda.api.dto.output.PaymentSummaryResponse;
import br.com.rinha.murillenda.domain.model.Payment;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ApplicationScoped
public class PaymentRepository {

    @Inject
    PgPool client; // Injeta o cliente reativo

    public Uni<Void> save(Payment payment) {
        return client.withTransaction(conn ->
                conn.preparedQuery("INSERT INTO payments (correlationId, amount, requested_at) VALUES ($1, $2, $3)")
                        .execute(Tuple.of(payment.correlationId(), payment.amount(), LocalDateTime.ofInstant(payment.requested_at(), java.time.ZoneOffset.UTC)))
        ).replaceWithVoid();
    }

    public Uni<PaymentSummaryResponse> getSummary() {
        Uni<Row> countUni = client.query("SELECT COUNT(*) as total_transactions FROM payments").execute()
                .map(rows -> rows.iterator().next());
        Uni<Row> sumUni = client.query("SELECT SUM(amount) as total_amount FROM payments").execute()
                .map(rows -> rows.iterator().next());

        return Uni.join().all(countUni, sumUni).andFailFast()
                .map(results -> {
                    Row countRow = results.get(0);
                    Row sumRow = results.get(1);

                    long count = countRow.getLong("total_transactions");
                    BigDecimal sum = sumRow.get(BigDecimal.class, "total_amount");

                    return new PaymentSummaryResponse(count, sum == null ? BigDecimal.ZERO : sum);
                });
    }
}