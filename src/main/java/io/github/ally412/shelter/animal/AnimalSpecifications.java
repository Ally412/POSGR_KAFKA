package io.github.ally412.shelter.animal;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AnimalSpecifications {
    private AnimalSpecifications() {}

    public static Specification<Animal> hasStatus(Status status) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.equal(root.get("status"), status);
    }
    public static Specification<Animal> hasSpecies(Species species) {
        return (root, query, criteriaBuilder)
                ->  criteriaBuilder.equal(root.get("species"), species);
    }
    public static Specification<Animal> nameContains(String fragment) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.like(root.get("name"), "%" + fragment + "%");
    }
    public static Specification<Animal> intakeFrom(LocalDate from) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.greaterThanOrEqualTo(root.get("intakeDate"), from);
    }
    public static Specification<Animal> intakeTo(LocalDate to) {
        return (root, query, criteriaBuilder)
                -> criteriaBuilder.lessThanOrEqualTo(root.get("intakeDate"), to);
    }

}
