package io.github.ally412.shelter.common.web;

public class NoSuchAnimalException extends RuntimeException {
    public NoSuchAnimalException(Long id) {
        super("Animal with " + id + " not found.");
    }
}
