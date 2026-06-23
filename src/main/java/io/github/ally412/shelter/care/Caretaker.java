package io.github.ally412.shelter.care;

import io.github.ally412.shelter.animal.Animal;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Caretaker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String name;
    @Email
    private String email;
    @NotNull
    @Enumerated(EnumType.STRING)
    private Specialization specialization;
    @ManyToMany(mappedBy = "caretakers")
    private Set<Animal> animals = new HashSet<>();
}
