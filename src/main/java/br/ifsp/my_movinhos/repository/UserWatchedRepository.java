package br.ifsp.my_movinhos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ifsp.my_movinhos.model.UserWatched;
import br.ifsp.my_movinhos.model.key.UserMovieId;

public interface UserWatchedRepository extends JpaRepository<UserWatched, UserMovieId> {

    Optional<UserWatched> findById(UserMovieId id);
    void deleteById(UserMovieId id);
    Page<UserWatched> findById_UserId(Long userId, Pageable pageable);
    boolean existsById_UserIdAndId_MovieId(Long userId, Long movieId);
    List<UserWatched> findById_UserId(Long userId);
}