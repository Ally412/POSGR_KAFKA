package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.Animal;
import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CaretakerRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    CaretakerRepository caretakerRepository;

    // ---------- findCaretakerWorkload (projection + GROUP BY/HAVING/ORDER BY) ----------
    @Test
    void findCaretakerWorkloadReportsCountsAtOrAboveMinBusiestFirst() {
        Caretaker busy = persistCaretaker("Busy", Specialization.VET);       // 3 animals
        Caretaker medium = persistCaretaker("Medium", Specialization.NANNY); // 2 animals
        Caretaker light = persistCaretaker("Light", Specialization.TRAINER); // 1 animal -> excluded by HAVING

        Animal a1 = persistAnimal("A1", Species.DOG, Status.AVAILABLE, LocalDate.now());
        Animal a2 = persistAnimal("A2", Species.DOG, Status.AVAILABLE, LocalDate.now());
        Animal a3 = persistAnimal("A3", Species.DOG, Status.AVAILABLE, LocalDate.now());

        link(a1, busy);
        link(a2, busy);
        link(a3, busy);
        link(a1, medium);
        link(a2, medium);
        link(a1, light);
        flushAndClear();

        List<CaretakerLoad> result = caretakerRepository.findCaretakerWorkload(2);

        // only Busy(3) and Medium(2), ordered busiest first; Light(1) filtered out
        assertThat(result).extracting(CaretakerLoad::name).containsExactly("Busy", "Medium");
        assertThat(result).extracting(CaretakerLoad::count).containsExactly(3L, 2L);
    }

    @Test
    void findCaretakerWorkloadReturnsEmptyWhenNoneMeetMin() {
        Caretaker light = persistCaretaker("Light", Specialization.TRAINER); // 1 animal
        Animal a1 = persistAnimal("A1", Species.DOG, Status.AVAILABLE, LocalDate.now());
        link(a1, light);
        flushAndClear();

        List<CaretakerLoad> result = caretakerRepository.findCaretakerWorkload(2);

        assertThat(result).isEmpty();
    }

    // ---------- helpers ----------
    private Animal persistAnimal(String name, Species species, Status status, LocalDate intakeDate) {
        Animal a = new Animal();
        a.setName(name);
        a.setSpecies(species);
        a.setBreed("n/a");
        a.setIntakeDate(intakeDate);
        a.setStatus(status);
        entityManager.persist(a);
        return a;
    }

    private Caretaker persistCaretaker(String name, Specialization specialization) {
        Caretaker c = new Caretaker();
        c.setName(name);
        c.setSpecialization(specialization);
        entityManager.persist(c);
        return c;
    }

    /** Animal owns the join table, so link from that side. */
    private void link(Animal animal, Caretaker caretaker) {
        animal.getCaretakers().add(caretaker);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
