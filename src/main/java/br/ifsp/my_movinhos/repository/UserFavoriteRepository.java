package br.ifsp.my_movinhos.repository;

import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.model.key.UserMovieId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for UserFavorite entity.
 * Provides standard CRUD operations and custom queries for user favorites.
 * Uses UserMovieId as the composite primary key.
 */
@Repository // Marks this interface as a Spring Data JPA repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UserMovieId> {
    Optional<UserFavorite> findById(UserMovieId id);
    void deleteById(UserMovieId id);
    Page<UserFavorite> findById_UserId(Long userId, Pageable pageable);
    boolean existsById_UserIdAndId_MovieId(Long userId, Long movieId);
}
