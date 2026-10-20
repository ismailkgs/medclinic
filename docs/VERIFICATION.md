# Готовность проекта и совместная проверка

Дата: 20 октября 2026 года, Asia/Bishkek.

Проект обновлён в исходной папке:
`C:/Users/Ismail/Documents/MyJavaProjects/medical-platform`.

## Результат

Готова учебная серверная реализация: расписание, бронирование, отмена, статусы и фильтры, Keycloak и проверка принадлежности, направления и запись на анализы, Kafka с Outbox/Inbox, годовые отчёты, очередь пятичасовых напоминаний, полный Docker-стенд и тесты.

- JUnit/Testcontainers: **15 тестов, 0 ошибок проверок, 0 исключений, 0 пропусков**.
- Сквозной сценарий с реальными токенами Keycloak: **23 проверки успешно**.
- Чистый импорт realm в отдельном временном Keycloak: успешно; проверены UUID пациента, роль PATIENT и audience medical-api.
- Совместимость с прежней PostgreSQL-базой: приложение запущено временно на 8083, миграция 008 прошла; история врача и анализы возвращают HTTP 200, отчёт за 2025 год READY. Временный процесс остановлен.
- Финальный Docker API: HTTP 200 на 8082; PostgreSQL и Kafka healthy.


**Настоящие SMS не отправлялись.** По умолчанию установлен режим log со статусом SIMULATED. HTTP-адаптер проверен локальным тестовым шлюзом; для настоящих SMS нужны совместимый провайдер/шлюз и доступ к нему. Получение сообщения лабораторией в стенде моделирует учебный Kafka-потребитель, внешняя лабораторная система не подключалась.

## Проверим вместе

1. Открой http://localhost:8082/api/health — ожидаем `Medical platform is running`.
2. В любом окне PowerShell выполни команду ниже.
3. Пришли последнюю строку или первую ошибку. Нормальный итог: `SUCCESS: 23 checks passed. Base URL: http://localhost:8082`.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:/Users/Ismail/Documents/MyJavaProjects/medical-platform/scripts/check-project.ps1"
```

Сценарий создаёт только учебные данные; отменяет свои проверочные приёмы и скрывает освободившиеся слоты. История и учебное направление на анализ остаются в отдельной Docker-базе. Если включён реальный HTTP SMS-адаптер, проверка близкого посещения с SMS пропускается.

## Два запуска

| Запуск | API | PostgreSQL | Kafka-топики |
|---|---|---|---|
| Docker, уже работает | localhost:8082 | отдельная база, порт 5433 | medical.docker.* |
| IDEA | localhost:8080 | прежняя установленная база, порт 5432 | medical.* |

Изменённые исходники попадут в запуск IDEA после остановки старого процесса и нового запуска MedicalPlatformApplication. Для готовой Docker-версии этого делать не нужно.

README.md в проекте содержит все адреса API, роли, ограничения, команды сборки и пояснение настоящей SMS-интеграции. В `scripts/run-docker.ps1` находится повторяемый запуск полного стенда. Пояснения к проекту сохранены отдельно на рабочем столе.

## Автоматические тесты

### kg.medical.platform.HttpSmsGatewayTests

- httpGatewaySendsUtf8AndStableIdempotencyKey — PASS
- httpGatewayDoesNotTreatFailureAsDelivery — PASS

### kg.medical.platform.MedicalPlatformApplicationTests

- validationAndSchedulePermissionsAreEnforced — PASS
- laboratoryWorkflowAndOwnershipAreEnforced — PASS
- adjacentSlotsAllowedButOverlapsRejectedAndSoftDeleteWorks — PASS
- keycloakRolesAreConvertedAndUnknownRolesIgnored — PASS
- cancellationPreservesHistoryAndSnapshotWhileAllowingRebooking — PASS
- concurrentBookingProducesExactlyOneWinner — PASS
- cancellationSuppressesAlreadyQueuedReminder — PASS
- completionRequiresStartedVisitAndFinalStatusCannotBeChanged — PASS
- annualReportRespectsBishkekYearBoundariesAndKafkaProcessing — PASS
- outboxRetriesFailureAndRealKafkaDeliveryIsIdempotent — PASS
- reminderIsQueuedAtFiveHoursAndSimulationIsNotReportedAsSent — PASS
- annualCatchUpRunsOnJanuary31AndDoesNotDuplicateJobs — PASS
- securitySeparatesAnonymousPatientDoctorAndAdmin — PASS

## Сквозные проверки

- Public health returns 200 — PASS
- Patient identity is linked — PASS
- Doctor identity is linked — PASS
- Anonymous history is rejected — PASS
- Patient cannot read doctor history — PASS
- Admin creates a patient profile — PASS
- Patient cannot access another profile — PASS
- Appointment is created — PASS
- Duplicate booking is rejected — PASS
- Occupied slot cannot be deleted — PASS
- Cancellation preserves the record — PASS
- Free slot can be rescheduled — PASS
- Free slot is removed from schedule — PASS
- Doctor creates a laboratory request — PASS
- Patient books the laboratory test — PASS
- Laboratory event passed through Kafka — PASS
- Annual report request is accepted — PASS
- Annual report is ready — PASS
- Reminder fixture appointment is created — PASS
- Reminder queue is processed — PASS
- SMS is honestly marked SIMULATED — PASS
- Reminder fixture is canceled — PASS
- Reminder fixture slot is hidden — PASS
