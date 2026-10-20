package kg.medical.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePatientRequest(
    @NotBlank(message = "Укажите фамилию")
    @Size(max = 100, message = "Фамилия должна содержать не более 100 символов")
    String lastName,
    @NotBlank(message = "Укажите имя")
    @Size(max = 100, message = "Имя должно содержать не более 100 символов")
    String firstName,
    @Size(max = 100, message = "Отчество должно содержать не более 100 символов")
    String patronymic,
    @NotBlank(message = "Укажите телефон")
    @Pattern(regexp = "[+][1-9][0-9]{7,14}", message = "Телефон должен начинаться с + и содержать от 8 до 15 цифр")
    String phone
) {
}
