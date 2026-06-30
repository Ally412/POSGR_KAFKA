package io.github.ally412.shelter.animal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AnimalPersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    TestEntityManager entityManager;
    @Autowired
    AnimalRepository animalRepository;

    @Test
    void savesAndReadsAnimal() {
        Animal animal = new Animal();
        animal.setName("Rex");
        animal.setSpecies(Species.DOG);
        animal.setBreed("Labrador");
        animal.setIntakeDate(LocalDate.now());
        animal.setStatus(Status.AVAILABLE);

        Long id = entityManager.persistAndFlush(animal).getId();
        entityManager.clear();

        Animal found = entityManager.find(Animal.class, id);

        assertNotNull(found);
        assertEquals("Rex", found.getName());
        assertEquals(Species.DOG, found.getSpecies());
        assertEquals(Status.AVAILABLE, found.getStatus());
    }
    @Test
    void fetchAnimalByStatus() {
        persistAnimal("Buddy", Species.DOG, Status.AVAILABLE);
        persistAnimal("Fluffy", Species.CAT, Status.AVAILABLE);
        persistAnimal("Wolfy", Species.DOG, Status.ADOPTED);
        List<Animal> animals = animalRepository.findByStatus(Status.AVAILABLE);
        assertEquals(2, animals.size());
        assertThat(animals)
                .extracting(Animal::getName)
                .containsExactlyInAnyOrder("Buddy", "Fluffy");

    }

    private Animal persistAnimal(String name, Species species, Status status) {
        Animal a = new Animal();
        a.setName(name);
        a.setSpecies(species);
        a.setBreed("n/a");
        a.setIntakeDate(LocalDate.now());
        a.setStatus(status);
        return entityManager.persistAndFlush(a);
    }
}
