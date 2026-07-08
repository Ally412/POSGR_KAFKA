package io.github.ally412.shelter.adoption;

import io.github.ally412.shelter.animal.Animal;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Getter
@Setter
public class Adoption {
    @Id
    private Long id;
    @NotNull
    private LocalDate date;
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Animal animal;
    @ManyToOne
    @JoinColumn(name = "adopter_id")
    private Adopter adopter;
}
