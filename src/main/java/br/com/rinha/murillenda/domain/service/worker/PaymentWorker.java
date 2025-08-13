package br.com.rinha.murillenda.domain.service.worker;

import br.com.rinha.murillenda.domain.model.Payment;
import br.com.rinha.murillenda.domain.repository.PaymentRepository;
import br.com.rinha.murillenda.domain.service.PaymentService;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Slf4j
public class PaymentWorker {

    @Inject
    PaymentRepository paymentRepository;

    @Inject
    PaymentService paymentService;

    @Scheduled(every = "5s")
    void processPaymentQueue() {
        log.info("Worker em ação: procurando por pagamentos pendentes...");

        paymentRepository.findAndLockPending(10)
                .onItem().transformToMulti(jobs -> Multi.createFrom().iterable(jobs))
                .group().intoLists().of(4)
                .onItem().transformToUniAndConcatenate(group -> {
                    log.info("Processando um novo grupo de {} jobs...", group.size());
                    List<Uni<Void>> tasks = new ArrayList<>();
                    for (Payment job : group) {
                        tasks.add(this.processSinglePaymentJob(job));
                    }
                    return Uni.join().all(tasks).andFailFast().replaceWithVoid();
                })
                .subscribe().with(
                        item -> {},
                        failure -> log.error("Erro no processamento da fila de pagamentos.", failure),
                        () -> log.info("Todos os grupos da fila foram processados.")
                );
    }

    private Uni<Void> processSinglePaymentJob(Payment job) {
        return paymentService.processQueuedPayment(job)
                .flatMap(result -> {
                    if (result.success()) {
                        return paymentRepository.updateStatusAndProcessor(job.correlationId(), "PROCESSED", result.processor());
                    }
                    return paymentRepository.updateStatus(job.correlationId(), "FAILED");
                });
    }
}