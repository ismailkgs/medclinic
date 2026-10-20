package kg.medical.platform.dto;

import java.time.Instant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
public record SlotRequest(
                           @NotNull(message = "Укажите начало приёма")
                           @Future(message = "Начало приёма должно быть в будущем")
                           Instant startsAt,
                           @NotNull(message = "Укажите окончание приёма")
                           @Future(message = "Окончание приёма должно быть в будущем")
                           Instant endsAt
) {
}
