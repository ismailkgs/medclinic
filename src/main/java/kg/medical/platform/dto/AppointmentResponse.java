package kg.medical.platform.dto;
import java.time.Instant;
import kg.medical.platform.entity.AppointmentStatus;

public record AppointmentResponse(
      Long id,
      PatientResponse patient,
      Long slotId,
      DoctorResponse doctor,
      AppointmentStatus status,
      Instant startsAt,
      Instant endsAt,
      Instant createdAt
) {
}
