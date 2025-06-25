package br.ifsp.my_movinhos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ifsp.my_movinhos.model.UserWatched;
import br.ifsp.my_movinhos.model.key.UserMovieId;

public interface UserWatchedRepository extends JpaRepository<UserWatched, UserMovieId> {
    void deleteByUserId(Long userId);
}
