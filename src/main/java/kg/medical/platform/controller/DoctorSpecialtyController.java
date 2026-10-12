package kg.medical.platform.controller;

import java.util.List;
import kg.medical.platform.dto.DoctorSpecialtyResponse;
import kg.medical.platform.service.DoctorSpecialtyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Регистрирует контроллер и включает запись возвращаемых данных в HTTP-ответ.

@RequestMapping("/api/specialties") // Задаёт адрес запросов к справочнику специальностей.

public class DoctorSpecialtyController { // Объявляет контроллер специальностей.
    private final DoctorSpecialtyService service; // Хранит ссылку на сервис.
    public DoctorSpecialtyController(DoctorSpecialtyService service) { // Объявляет конструктор, через который Spring передаст сервис.
        this.service = service; // Сохраняет сервис в поле контроллера.
    }

    @GetMapping // Обрабатывает GET-запрос непосредственно по адресу /api/specialties.
    public List<DoctorSpecialtyResponse> getAll() { // Объявляет метод получения списка специальностей.
        return service.getAll(); // Получает данные от сервиса и передаёт их Spring для формирования JSON-ответа.
    }
}