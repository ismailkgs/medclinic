package kg.medical.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity

@Table(name = "doctor_specialties")

public class DoctorSpecialty {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id", nullable = false)

    private Long id;
    @Column(name = "name", nullable = false, unique = true, length = 100)

    private String name;

    protected DoctorSpecialty() {
    }
    public DoctorSpecialty(String name) {
        this.name = name;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
}
