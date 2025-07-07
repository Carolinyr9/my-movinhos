package br.ifsp.my_movinhos.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

import br.ifsp.my_movinhos.model.common.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "watchlists")
public class Watchlist extends BaseEntity {
    @Setter
    private String name = "Watchlist";

    @Setter
    private String description;
    
    @Column(name = "user_id")
    private Long userId;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "watchlist_movies",
            joinColumns = @JoinColumn(name = "watchlist_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private Set<Movie> movies = new HashSet<>();

    public void addMovie(Movie movie) {
        this.movies.add(movie);
        movie.getWatchlists().add(this); 
    }

    public void removeMovie(Movie movie) {
        this.movies.remove(movie);
        movie.getWatchlists().remove(this); 
    }

    public Watchlist(String name, Long userId) {
        this.name = name;
        this.userId = userId;
    }
    
}
