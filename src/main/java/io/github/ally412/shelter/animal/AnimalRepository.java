package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.care.CaretakerLoad;
import io.github.ally412.shelter.care.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    @Query("SELECT a FROM Animal a WHERE a.status = :status")
    List<Animal> findByStatus(@Param("status") Status status);
    @Query("SELECT a FROM Animal a WHERE a.species = :species")
    List<Animal> findBySpecies(@Param("species") Species species);
    @Query("SELECT a FROM Animal a WHERE a.species = :species " +
            "AND a.status = io.github.ally412.shelter.animal.Status.AVAILABLE")
    List<Animal> findAvailableBySpecies(@Param("species") Species species);
    @Query("SELECT a FROM Animal a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :fragment, '%'))")
    List<Animal> searchByName(@Param("fragment")String fragment);
    @Query("SELECT a FROM Animal a WHERE a.intakeDate >= :from AND a.intakeDate <= :to")
    List<Animal> findIntakenBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
    @Query("SELECT a FROM Animal a ORDER BY a.intakeDate DESC")
    List<Animal> findRecentIntakes(Pageable pageable);
    @Query("SELECT a FROM Animal a WHERE a.adoption IS NULL")
    List<Animal> findNotAdopted();
    @Query("SELECT DISTINCT a FROM Animal a JOIN a.caretakers c WHERE c.specialization = :spec")
    List<Animal> findByCaretakerSpecialization(@Param("spec") Specialization spec);
    @Query("SELECT a.status, COUNT(a) FROM Animal a GROUP BY a.status")
    List<Object[]> countByStatus();
    @Query("SELECT a FROM Animal a WHERE a.medicalRecords IS EMPTY")
    List<Animal> findWithoutMedicalRecords();
    @Query("SELECT a FROM Animal a WHERE NOT EXISTS (SELECT m FROM MedicalRecord m WHERE m.animal = a)")
    List<Animal> findWithoutMedicalRecordsExists();
    @Query("SELECT new io.github.ally412.shelter.care.CaretakerLoad(c.name, COUNT(a)) " +
            "FROM Animal a JOIN a.caretakers c " +
            "GROUP BY c.id " +
            "HAVING COUNT(a) >= :min " +
            "ORDER BY COUNT(a) DESC")
    List<CaretakerLoad> findCaretakerWorkload(@Param("min") long min);
}
