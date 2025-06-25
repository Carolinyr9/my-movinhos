package br.ifsp.my_movinhos.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ifsp.my_movinhos.model.Genre;
import br.ifsp.my_movinhos.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTitle(String title);
    Optional<Movie> findByTitleIgnoreCase(String title);
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Movie> findByReleaseYear(int releaseYear, Pageable pageable);
    Page<Movie> findByGenresContaining(Genre genre, Pageable pageable);

    @Query(value = "SELECT DISTINCT m FROM Movie m JOIN m.genres g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :genreName, '%'))",
           countQuery = "SELECT COUNT(DISTINCT m) FROM Movie m JOIN m.genres g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :genreName, '%'))")
    Page<Movie> findByGenresNameContainingIgnoreCase(@Param("genreName") String genreName, Pageable pageable);
    
    boolean existsByGenresId(Long genreId);
}
