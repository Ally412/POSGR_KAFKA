package io.github.ally412.shelter.care.dto;

import io.github.ally412.shelter.care.Specialization;

public record CaretakerResponse(Long id,
                                String name,
                                String email,
                                Specialization specialization) {}
