package io.github.ally412.shelter.common.web;

public class AnimalAlreadyAdoptedException extends RuntimeException {
    public AnimalAlreadyAdoptedException(Long id) {
        super("Animal with " + id + " has already been adopted.");
    }
}
