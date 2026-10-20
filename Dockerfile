FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 appuser

WORKDIR /app

COPY --chown=appuser:appuser target/medical-platform-0.0.1-SNAPSHOT.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
