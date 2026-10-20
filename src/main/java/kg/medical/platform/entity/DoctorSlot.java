package kg.medical.platform.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity

@Table(name = "doctor_slots")

public class DoctorSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)

    private Doctor doctor;
    @Column(name = "starts_at", nullable = false)

    private Instant startsAt;
    @Column(name = "ends_at", nullable = false)

    private Instant endsAt;
    @Column(name = "deleted", nullable = false)

    private boolean deleted;

    protected DoctorSlot() {
    }

    public DoctorSlot(Doctor doctor, Instant startsAt, Instant endsAt) {
        this.doctor = doctor;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void reschedule(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void hide() { deleted = true; }
    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isDeleted() { return deleted; }
}
