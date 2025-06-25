package br.ifsp.my_movinhos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.model.key.UserMovieId;


public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UserMovieId> {
    boolean existsById(@NonNull UserMovieId favoriteId);
    void deleteByUserId(Long userId);
}
