package kg.medical.platform.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import kg.medical.platform.entity.Appointment;
import kg.medical.platform.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsBySlotIdAndStatusIn(Long slotId, Collection<AppointmentStatus> statuses);

    @Query("select a.slot.id from Appointment a where a.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)

    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findForUpdate(@Param("id") Long id);

    @Query("select a from Appointment a join fetch a.patient p join fetch a.slot s "
            + "join fetch s.doctor d join fetch d.specialty sp "
            + "where p.id = :patientId "
            + "and (:status is null or a.status = :status) "
            + "and (:specialtyId is null or sp.id = :specialtyId) "
            + "and (:doctorPattern is null or lower(concat(d.lastName, ' ', d.firstName, ' ', coalesce(d.patronymic, ''))) like :doctorPattern escape '!') "
            + "order by a.startsAt desc, a.id desc")
    List<Appointment> findPatientHistory(@Param("patientId") Long patientId, @Param("status") AppointmentStatus status, @Param("specialtyId") Long specialtyId, @Param("doctorPattern") String doctorPattern);
    @Query("select a from Appointment a join fetch a.patient p join fetch a.slot s "
            + "join fetch s.doctor d join fetch d.specialty "
            + "where d.id = :doctorId "
            + "and (:status is null or a.status = :status) "
            + "and (:patientPattern is null or lower(concat(p.lastName, ' ', p.firstName, ' ', coalesce(p.patronymic, ''))) like :patientPattern escape '!') "
            + "order by a.startsAt desc, a.id desc")
    List<Appointment> findDoctorHistory(@Param("doctorId") Long doctorId, @Param("status") AppointmentStatus status, @Param("patientPattern") String patientPattern);

}
