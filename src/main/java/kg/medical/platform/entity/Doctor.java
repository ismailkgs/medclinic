package kg.medical.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity

@Table(name = "doctors")
public class Doctor {
    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id", nullable = false)

    private Long id;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "patronymic", length = 100)
    private String patronymic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private DoctorSpecialty specialty;
    protected Doctor() {
    }
    public Doctor(String lastName, String firstName, String patronymic, DoctorSpecialty specialty) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.specialty = specialty;
    }
    public Long getId() {
        return id;
    }
    public String getLastName() {
        return lastName;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getPatronymic() {
        return patronymic;
    }
    public DoctorSpecialty getSpecialty() {
        return specialty;
    }
}
