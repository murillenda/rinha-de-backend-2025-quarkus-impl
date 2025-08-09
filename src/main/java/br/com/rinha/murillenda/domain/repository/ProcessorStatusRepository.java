package br.com.rinha.murillenda.domain.repository;

import br.com.rinha.murillenda.domain.model.ProcessorStatus;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class ProcessorStatusRepository {

    @Inject
    PgPool client;

    public Uni<Optional<ProcessorStatus>> findById(String processorName) {
        return client.preparedQuery("SELECT * FROM processor_status WHERE processorName = $1")
                .execute(Tuple.of(processorName))
                .map(rows -> {
                    if (rows.size() == 0) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRowToProcessorStatus(rows.iterator().next()));
                });
    }

    public Uni<Void> update(ProcessorStatus status) {
        return client.withTransaction(conn ->
                conn.preparedQuery("INSERT INTO processor_status (processorName, isHealthy, lastChecked) " +
                                "VALUES ($1, $2, $3) " +
                                "ON CONFLICT (processorName) DO UPDATE " +
                                "SET isHealthy = $2, lastChecked = $3")
                        .execute(Tuple.of(status.processorName(), status.isHealthy(), LocalDateTime.ofInstant(status.lastChecked(), java.time.ZoneOffset.UTC)))
        ).replaceWithVoid();
    }

    private static ProcessorStatus mapRowToProcessorStatus(Row row) {
        return new ProcessorStatus(
                row.getString("processorname"),
                row.getBoolean("ishealthy"),
                row.getLocalDateTime("lastchecked").toInstant(java.time.ZoneOffset.UTC)
        );
    }
}