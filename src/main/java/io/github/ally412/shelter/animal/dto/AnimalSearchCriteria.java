package io.github.ally412.shelter.animal.dto;

import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;

import java.time.LocalDate;

public record AnimalSearchCriteria(Status status,
                                   Species species,
                                   String nameFragment,
                                   LocalDate intakeFrom,
                                   LocalDate intakeTo) {}
