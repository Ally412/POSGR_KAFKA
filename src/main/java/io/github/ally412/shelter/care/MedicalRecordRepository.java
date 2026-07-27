package io.github.ally412.shelter.care;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.animal.id = :animalId")
    List<MedicalRecord> findByAnimalId(@Param("animalId") Long animalId);
    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.id = :id")
    Optional<MedicalRecord> findById(@Param("id") Long id);
}
