package kg.medical.platform.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import kg.medical.platform.entity.AppointmentStatus;
import kg.medical.platform.entity.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorSlot s where s.id = :id")
    Optional<DoctorSlot> findForUpdate(@Param("id") Long id);
    @Query("select count(s) from DoctorSlot s where s.doctor.id = :doctorId and s.deleted = false "
            + "and s.startsAt < :endsAt and s.endsAt > :startsAt")
    long countOverlapping(@Param("doctorId") Long doctorId, @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);
    @Query("select s from DoctorSlot s where s.doctor.id = :doctorId and s.deleted = false and s.startsAt > :now "
            + "and not exists (select a.id from Appointment a where a.slot = s and a.status in :statuses) "
            + "order by s.startsAt, s.id")
    List<DoctorSlot> findFree(@Param("doctorId") Long doctorId, @Param("now") Instant now, @Param("statuses") Collection<AppointmentStatus> statuses);

    @Query("select count(s) from DoctorSlot s where s.doctor.id = :doctorId and s.deleted = false "
            + "and s.id <> :slotId and s.startsAt < :endsAt and s.endsAt > :startsAt")
    long countOtherOverlapping(@Param("doctorId") Long doctorId, @Param("slotId") Long slotId, @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);
}
