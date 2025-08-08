package br.com.rinha.murillenda.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processor_status")
public class ProcessorStatus extends PanacheEntityBase {

    @Id
    public String processorName;
    public boolean isHealthy;
    public Instant lastChecked;

}
