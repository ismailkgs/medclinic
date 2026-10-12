package kg.medical.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController // Регистрирует класс в Spring и указывает, что результаты методов нужно помещать в тело HTTP-ответа.
@RequestMapping("/api") // общий префикс /api для адресов этого контроллера.

public class HealthController { // Объявляем класс, который обрабатывает запросы проверки приложения.

    @GetMapping("/health") // Связывает HTTP-запрос GET /api/health с методом health.

    public String health() { // создаём метод, возвращающий строку с результатом проверки.
        return "Medical platform is running"; // возвращАем текст, который клиент получит в теле HTTP-ответа.
    }
}