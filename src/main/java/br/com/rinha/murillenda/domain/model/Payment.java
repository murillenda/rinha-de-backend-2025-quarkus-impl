package br.com.rinha.murillenda.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "payments")
public class Payment extends PanacheEntityBase {

    @Id
    public UUID correlationId; // Chave primária
    public BigDecimal amount;
    public Instant requested_at;

}
