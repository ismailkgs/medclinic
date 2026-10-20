package kg.medical.platform.dto;

public record PatientResponse(
                               Long id,
                               String lastName,
                               String firstName,
                               String patronymic,
                               String phone
) {
}
