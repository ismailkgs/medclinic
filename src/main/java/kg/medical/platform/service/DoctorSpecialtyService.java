package kg.medical.platform.service;

import java.util.List;
import kg.medical.platform.dto.DoctorSpecialtyResponse;
import kg.medical.platform.repository.DoctorSpecialtyRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service

public class DoctorSpecialtyService {
    private final DoctorSpecialtyRepository repository;

    public DoctorSpecialtyService(DoctorSpecialtyRepository repository) {
        this.repository = repository;
    }
    @Transactional(readOnly = true)

    public List<DoctorSpecialtyResponse> getAll() {
        return repository.findAll(Sort.by("name")).stream()
                .map(specialty -> new DoctorSpecialtyResponse(specialty.getId(), specialty.getName()))
                .toList();
    }
}
