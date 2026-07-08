package io.github.ally412.shelter.animal.dto;


import io.github.ally412.shelter.animal.Species;
import io.github.ally412.shelter.animal.Status;

import java.time.LocalDate;

public record AnimalResponse (Long id,
                              String name,
                              Species species,
                              String breed,
                              LocalDate intakeDate,
                              Status status) {}
