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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AnimalPersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    TestEntityManager entityManager;

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
}
