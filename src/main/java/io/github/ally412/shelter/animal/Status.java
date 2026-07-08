package io.github.ally412.shelter.animal;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.ally412.shelter.common.web.InvalidStatusException;
import jakarta.validation.constraints.NotNull;

@NotNull
public enum Status {
    ADOPTED,
    AVAILABLE,
    SOCIALIZING;

    @JsonCreator
    public static Status parseStatus(String status) {
        try {
            return Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException(status);
        }
    }
}
