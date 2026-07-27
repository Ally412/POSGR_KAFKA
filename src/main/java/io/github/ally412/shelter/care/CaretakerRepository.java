package io.github.ally412.shelter.care;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaretakerRepository extends JpaRepository<Caretaker, Long> {
    @Query("SELECT c FROM Caretaker c JOIN c.animals a WHERE a.id = :animalId")
    List<Caretaker> findByAnimalId(@Param("animalId") Long id);
    @Query("SELECT new io.github.ally412.shelter.care.CaretakerLoad(c.name, COUNT(a)) " +
            "FROM Animal a JOIN a.caretakers c " +
            "GROUP BY c.id " +
            "HAVING COUNT(a) >= :min " +
            "ORDER BY COUNT(a) DESC")
    List<CaretakerLoad> findCaretakerWorkload(@Param("min") long min);
}
