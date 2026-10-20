package kg.medical.platform.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import kg.medical.platform.entity.Doctor;
import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    @Query("select d from Doctor d join fetch d.specialty order by d.lastName, d.firstName, d.id")
    List<Doctor> findAllWithSpecialty();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findForUpdate(@Param("id") Long id);
}
