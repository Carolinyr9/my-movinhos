package br.ifsp.my_movinhos.exception;

public class InvalidReviewStateException extends RuntimeException {
    public InvalidReviewStateException(String message) {
        super(message);
    }
}
