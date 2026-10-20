package kg.medical.platform.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private DoctorSlot slot;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AppointmentStatus status;
    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt;
    @Column(name = "ends_at", nullable = false, updatable = false)
    private Instant endsAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    protected Appointment() {
    }
    public Appointment(Patient patient, DoctorSlot slot) {
        this.patient = patient;
        this.slot = slot;
        this.status = AppointmentStatus.CREATED;
        this.startsAt = slot.getStartsAt();
        this.endsAt = slot.getEndsAt();
    }
    public void cancel() {
        requireCreated();
        status = AppointmentStatus.CANCELED;
    }
    public void complete() {
        requireCreated();
        status = AppointmentStatus.COMPLETED;
    }
    private void requireCreated() {
        if (status != AppointmentStatus.CREATED) {
            throw new IllegalStateException("Изменить можно только запись со статусом CREATED");
        }
    }
    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public DoctorSlot getSlot() { return slot; }
    public AppointmentStatus getStatus() { return status; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
