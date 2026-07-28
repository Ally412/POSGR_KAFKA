package io.github.ally412.shelter.animal;

import io.github.ally412.shelter.animal.dto.AnimalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@Transactional
class AnimalServiceCachingIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    AnimalService animalService;

    // Spy the REAL repository so we can count how often the DB layer is actually hit.
    @MockitoSpyBean
    AnimalRepository animalRepository;

    @Autowired
    CacheManager cacheManager;

    // The cache is a shared singleton — NOT rolled back with the transaction. Clear it per test.
    @BeforeEach
    void clearCache() {
        cacheManager.getCache("animals").clear();
    }

    @Test
    void secondGetIsServedFromCacheAndSkipsRepository() {
        Long id = persistAnimal("Buddy");

        animalService.getAnimal(id);   // miss → hits repository, caches result
        animalService.getAnimal(id);   // hit  → served from cache, repository NOT touched

        verify(animalRepository, times(1)).findById(id);
    }

    @Test
    void updateEvictsSoNextGetRefetches() {
        Long id = persistAnimal("Buddy");
        animalService.getAnimal(id);   // caches under key = id

        animalService.updateAnimal(id, new AnimalRequest("Renamed", Species.DOG, "Mix", Status.AVAILABLE));

        // Ignore the findById that update() did internally; measure only the post-update read.
        clearInvocations(animalRepository);
        Animal refreshed = animalService.getAnimal(id).orElseThrow();

        // If the key had NOT been evicted this read would come from cache → 0 repository calls.
        verify(animalRepository, times(1)).findById(id);   // the real proof eviction happened
        assertThat(refreshed.getName()).isEqualTo("Renamed");
    }

    @Test
    void deleteEvictsCachedEntry() {
        Long id = persistAnimal("Buddy");
        animalService.getAnimal(id);   // caches

        animalService.deleteAnimal(id);

        clearInvocations(animalRepository);
        assertThat(animalService.getAnimal(id)).isEmpty();   // gone from DB
        verify(animalRepository, times(1)).findById(id);     // and cache was evicted, so it re-checked
    }

    @Test
    void emptyResultIsNotCached() {
        // unless = "#result == null" means a miss is never stored, so every lookup re-hits the DB.
        animalService.getAnimal(999L);
        animalService.getAnimal(999L);

        verify(animalRepository, times(2)).findById(999L);
    }

    private Long persistAnimal(String name) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setSpecies(Species.DOG);
        animal.setBreed("Mix");
        animal.setIntakeDate(LocalDate.now());
        animal.setStatus(Status.SOCIALIZING);
        return animalRepository.save(animal).getId();
    }
}
