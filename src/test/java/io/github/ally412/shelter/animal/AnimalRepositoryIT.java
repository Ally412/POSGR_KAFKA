package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.adoption.Adoption;
import io.github.ally412.shelter.care.Caretaker;
import io.github.ally412.shelter.care.CaretakerLoad;
import io.github.ally412.shelter.care.MedicalRecord;
import io.github.ally412.shelter.care.Specialization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AnimalRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    AnimalRepository animalRepository;

    // ---------- #1 findBySpecies ----------
    @Test
    void findBySpeciesReturnsOnlyThatSpecies() {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Bruno", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Whiskers", Species.CAT, Status.AVAILABLE, LocalDate.now());
        flushAndClear();

        List<Animal> dogs = animalRepository.findBySpecies(Species.DOG);

        assertThat(dogs).extracting(Animal::getName)
                .containsExactlyInAnyOrder("Rex", "Bruno");
    }

    // ---------- #2 findByStatus ----------
    @Test
    void findByStatusReturnsOnlyThatStatus() {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Milo", Species.CAT, Status.ADOPTED, LocalDate.now());
        flushAndClear();

        List<Animal> available = animalRepository.findByStatus(Status.AVAILABLE);

        assertThat(available).extracting(Animal::getName).containsExactly("Rex");
    }

    // ---------- #3 findAvailableBySpecies ----------
    @Test
    void findAvailableBySpeciesMatchesSpeciesAndAvailableStatus() {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());     // match
        persistAnimal("Bruno", Species.DOG, Status.ADOPTED, LocalDate.now());     // wrong status
        persistAnimal("Whiskers", Species.CAT, Status.AVAILABLE, LocalDate.now()); // wrong species
        flushAndClear();

        List<Animal> result = animalRepository.findAvailableBySpecies(Species.DOG);

        assertThat(result).extracting(Animal::getName).containsExactly("Rex");
    }

    // ---------- #4 searchByName (case-insensitive contains) ----------
    @Test
    void searchByNameIsCaseInsensitiveAndMatchesFragment() {
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("rexy", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Bruno", Species.DOG, Status.AVAILABLE, LocalDate.now());
        flushAndClear();

        List<Animal> result = animalRepository.searchByName("REX");

        assertThat(result).extracting(Animal::getName)
                .containsExactlyInAnyOrder("Rex", "rexy");
    }

    // ---------- #5 findIntakenBetween (inclusive both ends) ----------
    @Test
    void findIntakenBetweenIsInclusiveOnBothBounds() {
        persistAnimal("Before", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-05-01"));
        persistAnimal("OnFrom", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-06-01"));
        persistAnimal("Middle", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-06-15"));
        persistAnimal("OnTo", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-12-01"));
        persistAnimal("After", Species.DOG, Status.AVAILABLE, LocalDate.parse("2027-01-01"));
        flushAndClear();

        List<Animal> result = animalRepository.findIntakenBetween(
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-12-01"));

        assertThat(result).extracting(Animal::getName)
                .containsExactlyInAnyOrder("OnFrom", "Middle", "OnTo");
    }

    // ---------- #6 findRecentIntakes (ordered DESC, limited by Pageable) ----------
    @Test
    void findRecentIntakesReturnsNewestFirstLimited() {
        persistAnimal("Old", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-01-01"));
        persistAnimal("Mid", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-06-01"));
        persistAnimal("New", Species.DOG, Status.AVAILABLE, LocalDate.parse("2026-12-01"));
        flushAndClear();

        List<Animal> result = animalRepository.findRecentIntakes(PageRequest.of(0, 2));

        // exactly two, newest first — order matters here
        assertThat(result).extracting(Animal::getName).containsExactly("New", "Mid");
    }

    // ---------- #7 findNotAdopted (IS NULL on the OneToOne) ----------
    @Test
    void findNotAdoptedExcludesAnimalsWithAdoption() {
        Animal adopted = persistAnimal("Adopted", Species.DOG, Status.ADOPTED, LocalDate.now());
        persistAnimal("Free", Species.CAT, Status.AVAILABLE, LocalDate.now());
        persistAdoption(adopted, LocalDate.now());
        flushAndClear();

        List<Animal> result = animalRepository.findNotAdopted();

        assertThat(result).extracting(Animal::getName).containsExactly("Free");
    }

    // ---------- #8 findByCaretakerSpecialization (JOIN + DISTINCT) ----------
    @Test
    void findByCaretakerSpecializationJoinsAndDeduplicates() {
        Caretaker vet1 = persistCaretaker("Vet One", Specialization.VET);
        Caretaker vet2 = persistCaretaker("Vet Two", Specialization.VET);
        Caretaker nanny = persistCaretaker("Nanny", Specialization.NANNY);

        Animal twoVets = persistAnimal("TwoVets", Species.DOG, Status.AVAILABLE, LocalDate.now());
        Animal oneVet = persistAnimal("OneVet", Species.CAT, Status.AVAILABLE, LocalDate.now());
        Animal nannyOnly = persistAnimal("NannyOnly", Species.DOG, Status.AVAILABLE, LocalDate.now());

        link(twoVets, vet1);
        link(twoVets, vet2);   // two VET caretakers -> DISTINCT must collapse to one row
        link(oneVet, vet1);
        link(nannyOnly, nanny);
        flushAndClear();

        List<Animal> result = animalRepository.findByCaretakerSpecialization(Specialization.VET);

        assertThat(result).extracting(Animal::getName)
                .containsExactlyInAnyOrder("TwoVets", "OneVet"); // each once, nannyOnly excluded
    }

    // ---------- #9 countByStatus (COUNT + GROUP BY) ----------
    @Test
    void countByStatusGroupsAndCounts() {
        persistAnimal("A1", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("A2", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("B1", Species.CAT, Status.ADOPTED, LocalDate.now());
        flushAndClear();

        Map<Status, Long> counts = animalRepository.countByStatus().stream()
                .collect(Collectors.toMap(row -> (Status) row[0], row -> (Long) row[1]));

        assertThat(counts)
                .containsEntry(Status.AVAILABLE, 2L)
                .containsEntry(Status.ADOPTED, 1L)
                .doesNotContainKey(Status.SOCIALIZING); // zero-count groups are absent
    }

    // ---------- #10 findWithoutMedicalRecords (both forms) ----------
    @Test
    void findWithoutMedicalRecordsReturnsAnimalsWithNoRecords() {
        Animal withRecord = persistAnimal("Sick", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Healthy", Species.CAT, Status.AVAILABLE, LocalDate.now());
        persistMedicalRecord(withRecord, "checkup", "Dr. Vet", LocalDate.now());
        flushAndClear();

        assertThat(animalRepository.findWithoutMedicalRecords())
                .extracting(Animal::getName).containsExactly("Healthy");
    }

    @Test
    void findWithoutMedicalRecordsExistsMatchesTheIsEmptyForm() {
        Animal withRecord = persistAnimal("Sick", Species.DOG, Status.AVAILABLE, LocalDate.now());
        persistAnimal("Healthy", Species.CAT, Status.AVAILABLE, LocalDate.now());
        persistMedicalRecord(withRecord, "checkup", "Dr. Vet", LocalDate.now());
        flushAndClear();

        assertThat(animalRepository.findWithoutMedicalRecordsExists())
                .extracting(Animal::getName).containsExactly("Healthy");
    }

    // ---------- #11 findCaretakerWorkload (projection + GROUP BY/HAVING/ORDER BY) ----------
    @Test
    void findCaretakerWorkloadReportsCountsAtOrAboveMinBusiestFirst() {
        Caretaker busy = persistCaretaker("Busy", Specialization.VET);     // 3 animals
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

        List<CaretakerLoad> result = animalRepository.findCaretakerWorkload(2);

        // only Busy(3) and Medium(2), ordered busiest first; Light(1) filtered out
        assertThat(result).extracting(CaretakerLoad::name).containsExactly("Busy", "Medium");
        assertThat(result).extracting(CaretakerLoad::count).containsExactly(3L, 2L);
    }

    // ---------- #12 averageDaysInShelterBySpecies (native SQL) ----------
    @Test
    void averageDaysInShelterBySpeciesComputesPerSpeciesAverage() {
        LocalDate today = LocalDate.now();
        persistAnimal("Rex", Species.DOG, Status.AVAILABLE, today.minusDays(10));
        persistAnimal("Bruno", Species.DOG, Status.AVAILABLE, today.minusDays(20)); // dog avg = 15
        persistAnimal("Whiskers", Species.CAT, Status.AVAILABLE, today.minusDays(4)); // cat avg = 4
        flushAndClear();

        // Native query bypasses entity mapping: species comes back as a raw String
        // (not the Species enum) and AVG returns a BigDecimal (not a double).
        Map<String, BigDecimal> avgBySpecies = animalRepository.averageDaysInShelterBySpecies().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (BigDecimal) row[1]));

        assertThat(avgBySpecies.get("DOG")).isEqualByComparingTo("15");
        assertThat(avgBySpecies.get("CAT")).isEqualByComparingTo("4");
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

    private void persistMedicalRecord(Animal animal, String description, String vetName, LocalDate date) {
        MedicalRecord r = new MedicalRecord();
        r.setAnimal(animal);
        r.setDescription(description);
        r.setVetName(vetName);
        r.setTreatmentDate(date);
        entityManager.persist(r);
    }

    private void persistAdoption(Animal animal, LocalDate date) {
        Adoption adoption = new Adoption();
        adoption.setAnimal(animal);
        adoption.setDate(date);
        entityManager.persist(adoption);
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
