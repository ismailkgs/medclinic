package kg.medical.platform.dto;
public record DoctorResponse(
   Long id,
   String lastName,
   String firstName,
   String patronymic,
   DoctorSpecialtyResponse specialty
) {
}
