FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS builder
USER root

WORKDIR /home/quarkus/app

COPY . .

RUN chmod +x ./mvnw

RUN ./mvnw package -Pnative


FROM quay.io/quarkus/quarkus-micro-image:2.0
WORKDIR /work/

COPY --from=builder /home/quarkus/app/target/*-runner /work/application

EXPOSE 8080

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]