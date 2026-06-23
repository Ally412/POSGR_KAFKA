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
    @OneToMany(mappedBy = "animal")
    private Set<MedicalRecord> medicalRecords = new HashSet<>();
    @OneToOne(mappedBy = "animal")
    private Adoption adoption;
    @ManyToMany
    @JoinTable(
            name = "animal_caretaker",
            joinColumns = @JoinColumn(name = "animal_id"),
            inverseJoinColumns = @JoinColumn(name = "caretaker_id")
    )
    private Set<Caretaker> caretakers = new HashSet<>();
}
