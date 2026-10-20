package kg.medical.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAppointmentRequest(
                                        @NotNull(message = "Укажите слот")
                                        @Positive(message = "Идентификатор слота должен быть положительным")
                                        Long slotId
) {
}
