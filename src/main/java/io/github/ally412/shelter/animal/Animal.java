package io.github.ally412.shelter.animal;


import io.github.ally412.shelter.adoption.Adoption;
import io.github.ally412.shelter.care.Caretaker;
import io.github.ally412.shelter.care.MedicalRecord;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Species species;
    @NotBlank
    private String breed;
    @NotNull
    private LocalDate intakeDate;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;
    // Children of the Animal aggregate: they have no life of their own, so the animal's
    // lifecycle is theirs. orphanRemoval makes dropping one from the set delete its row —
    // without it, removing from a mappedBy collection emits no SQL at all.
    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MedicalRecord> medicalRecords = new HashSet<>();
    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Adoption adoption;
    // No cascade: caretakers are shared between animals and outlive any one of them.
    @ManyToMany
    @JoinTable(
            name = "animal_caretaker",
            joinColumns = @JoinColumn(name = "animal_id"),
            inverseJoinColumns = @JoinColumn(name = "caretaker_id")
    )
    private Set<Caretaker> caretakers = new HashSet<>();
}
