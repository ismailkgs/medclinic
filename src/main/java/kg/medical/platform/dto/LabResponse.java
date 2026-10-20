package kg.medical.platform.dto;
import java.time.Instant;
public record LabResponse(Long id, Long doctorId, Long patientId, String testCode, String status, Instant scheduledAt, Instant createdAt, Instant dispatchedAt) { }
