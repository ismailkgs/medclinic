package kg.medical.platform.service;
import java.util.List;
import kg.medical.platform.dto.DoctorResponse;
import kg.medical.platform.dto.DoctorSpecialtyResponse;
import kg.medical.platform.entity.Doctor;
import kg.medical.platform.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {
    private final DoctorRepository repository;
    public DoctorService(DoctorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAll() {
        return repository.findAllWithSpecialty().stream()
                .map(this::toResponse)
                .toList();
    }

    private DoctorResponse toResponse(Doctor doctor) {
        DoctorSpecialtyResponse specialty = new DoctorSpecialtyResponse(
                doctor.getSpecialty().getId(),
                doctor.getSpecialty().getName()
        );

        return new DoctorResponse(
                doctor.getId(),
                doctor.getLastName(),
                doctor.getFirstName(),
                doctor.getPatronymic(),
                specialty
        );
    }
}
