package br.ifsp.my_movinhos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import br.ifsp.my_movinhos.model.key.UserMovieId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"movie", "review"})
@Table(name = "user_watcheds")
public class UserWatched {
    @EmbeddedId
    private UserMovieId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    private Movie movie;

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;

    @OneToOne(
            mappedBy = "userWatched",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Review review;

    public UserWatched(Long userId, Movie movie, LocalDateTime watchedAt) {
        // Inicializa a chave composta UserMovieId com o userId e o ID do filme.
        this.id = new UserMovieId(userId, movie.getId());
        this.movie = movie;
        this.watchedAt = watchedAt;
    }

    public Long getUserId() {
        return this.id != null ? this.id.getUserId() : null;
    }

    public void addReview(String content, int directionScore, int screenplayScore, int cinematographyScore, int generalScore) {
        if (this.review == null) { 
            Review newReview = new Review(this, content);
            newReview.setDirectionScore(directionScore);
            newReview.setScreenplayScore(screenplayScore);
            newReview.setCinematographyScore(cinematographyScore);
            newReview.setGeneralScore(generalScore);
            this.review = newReview;
        }
    }

    public void removeReview() {
        this.review = null;
    }
}
