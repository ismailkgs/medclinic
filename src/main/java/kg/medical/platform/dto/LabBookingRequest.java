package kg.medical.platform.dto;
import java.time.Instant;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
public record LabBookingRequest(@NotNull @Future Instant scheduledAt) { }
