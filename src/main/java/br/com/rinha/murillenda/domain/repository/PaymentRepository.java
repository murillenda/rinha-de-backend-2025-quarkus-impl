package br.com.rinha.murillenda.domain.repository;

import br.com.rinha.murillenda.domain.model.Payment;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class PaymentRepository implements PanacheRepositoryBase<Payment, UUID> {
}
