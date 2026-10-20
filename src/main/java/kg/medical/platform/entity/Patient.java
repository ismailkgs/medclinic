package kg.medical.platform.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity

@Table(name = "patients")

public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "patronymic", length = 100)
    private String patronymic;
    @Column(name = "phone", nullable = false, length = 32)
    private String phone;
    @Column(name = "keycloak_subject", unique = true, length = 255)
    private String keycloakSubject;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    protected Patient() {
    }

    public Patient(String lastName, String firstName, String patronymic, String phone) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getPatronymic() { return patronymic; }
    public String getPhone() { return phone; }
    public String getKeycloakSubject() { return keycloakSubject; }
    public Instant getCreatedAt() { return createdAt; }
}
