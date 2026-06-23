package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.Animal;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Getter
@Setter
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "animal_id")
    private Animal animal;
    @NotBlank
    private String description;
    @NotNull
    private LocalDate treatmentDate;
    @NotBlank
    private String vetName; // doctor name


}
