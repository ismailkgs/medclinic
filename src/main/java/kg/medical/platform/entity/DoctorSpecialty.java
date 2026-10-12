package kg.medical.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity // Сообщает JPA, что объекты этого класса представляют строки базы данных.

@Table(name = "doctor_specialties") // Связывает класс с таблицей doctor_specialties, созданной Liquibase.

public class DoctorSpecialty {

    @Id // Обозначает поле как первичный ключ сущности.

    @GeneratedValue(strategy = GenerationType.IDENTITY)// Поручает PostgreSQL создавать идентификатор при вставке строки.

    @Column(name = "id", nullable = false) // Связывает поле со столбцом id, в котором запрещён NULL.

    private Long id; // Хранит идентификатор; до сохранения нового объекта он может быть null.
    @Column(name = "name", nullable = false, unique = true, length = 100) // Описывает столбец названия с ограничениями из миграции.

    private String name; // Хранит название специальности, например «Терапевт».

    protected DoctorSpecialty() { // Создаёт конструктор без аргументов, который нужен JPA для загрузки объектов.
    }
    public DoctorSpecialty(String name) { // Позволяет нашему коду создать новую специальность с названием.
        this.name = name; // Записывает переданное название в поле текущего объекта.
    }
    public Long getId() { // Объявляет метод чтения идентификатора.
        return id; // Возвращает идентификатор текущей специальности.
    }
    public String getName() { // Объявляет метод чтения названия.
        return name; // Возвращает название текущей специальности.
    }
}