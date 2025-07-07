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
@ToString(exclude = {"movie"})
@Table(name = "user_favorites")
public class UserFavorite {
    @EmbeddedId
    private UserMovieId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    private Movie movie;

    @Column(name = "favorited_at", nullable = false)
    private LocalDateTime favoritedAt;

    public UserFavorite(Long userId, Movie movie) {
        this.id = new UserMovieId(userId, movie.getId());
        this.movie = movie;
        this.favoritedAt = LocalDateTime.now();
    }

    public Long getUserId() {
        return this.id != null ? this.id.getUserId() : null;
    }
}
