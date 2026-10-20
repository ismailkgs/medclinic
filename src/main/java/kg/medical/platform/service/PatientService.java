package kg.medical.platform.service;

import kg.medical.platform.dto.CreatePatientRequest;
import kg.medical.platform.dto.PatientResponse;
import kg.medical.platform.entity.Patient;
import kg.medical.platform.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {
    private final PatientRepository repository;
    public PatientService(PatientRepository repository) {
        this.repository = repository;
    }
    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        String patronymic = request.patronymic() == null ? null : request.patronymic().strip();
        Patient patient = new Patient(request.lastName().strip(), request.firstName().strip(), patronymic, request.phone());
        Patient saved = repository.save(patient);
        return new PatientResponse(saved.getId(), saved.getLastName(), saved.getFirstName(), saved.getPatronymic(), saved.getPhone());
    }

}
