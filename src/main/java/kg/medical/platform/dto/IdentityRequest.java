package kg.medical.platform.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record IdentityRequest(@NotBlank @Size(max=255) String subject) { }
