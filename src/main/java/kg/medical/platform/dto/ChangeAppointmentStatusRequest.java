package kg.medical.platform.dto;

import jakarta.validation.constraints.NotNull;
import kg.medical.platform.entity.AppointmentStatus;

public record ChangeAppointmentStatusRequest(
        @NotNull(message = "Укажите статус")
        AppointmentStatus status
) {
}
