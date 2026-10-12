package kg.medical.platform.service;

import java.util.List;
import kg.medical.platform.dto.DoctorSpecialtyResponse;
import kg.medical.platform.repository.DoctorSpecialtyRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service // Регистрирует экземпляр сервиса в контейнере Spring.

public class DoctorSpecialtyService {
    private final DoctorSpecialtyRepository repository; // Хранит ссылку на репозиторий, назначаемую при создании сервиса.

    public DoctorSpecialtyService(DoctorSpecialtyRepository repository) { // Объявляет конструктор, через который Spring передаст репозиторий.
        this.repository = repository; // Сохраняет переданный репозиторий в поле сервиса.
    }
    @Transactional(readOnly = true) // Выполняет чтение и преобразование данных в транзакции с режимом только чтения.

    public List<DoctorSpecialtyResponse> getAll() { // Объявляет метод получения списка специальностей для API.
        return repository.findAll(Sort.by("name")).stream() // Загружает сущности с сортировкой по названию и создаёт поток для преобразования.
                .map(specialty -> new DoctorSpecialtyResponse(specialty.getId(), specialty.getName())) // Преобразуем каждую сущность в объект ответа.
                .toList(); // Собирает объекты ответа в список и возвращает его.
    }
}