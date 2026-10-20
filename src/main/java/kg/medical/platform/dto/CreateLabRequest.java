package kg.medical.platform.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
public record CreateLabRequest(@NotNull @Positive Long patientId, @NotNull @Pattern(regexp="[A-Z][A-Z0-9_]{0,63}",message="Код анализа: заглавные латинские буквы, цифры и _") String testCode) { }
