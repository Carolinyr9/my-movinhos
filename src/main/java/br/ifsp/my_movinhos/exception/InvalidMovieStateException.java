package br.ifsp.my_movinhos.exception;

public class InvalidMovieStateException extends RuntimeException {
    public InvalidMovieStateException(String message) {
        super(message);
    }
}
