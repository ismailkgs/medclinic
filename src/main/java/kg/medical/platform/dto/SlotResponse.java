package kg.medical.platform.dto;
import java.time.Instant;

public record SlotResponse(
                            Long id,
                            Long doctorId,
                            Instant startsAt,
                            Instant endsAt
) {
}
