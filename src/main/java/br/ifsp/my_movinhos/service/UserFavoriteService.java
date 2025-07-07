package br.ifsp.my_movinhos.service;

import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.model.Movie;
import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.model.key.UserMovieId;
import br.ifsp.my_movinhos.repository.MovieRepository;
import br.ifsp.my_movinhos.repository.UserFavoriteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service 
public class UserFavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;
    private final MovieRepository movieRepository; 

    @Autowired
    public UserFavoriteService(UserFavoriteRepository userFavoriteRepository, MovieRepository movieRepository) {
        this.userFavoriteRepository = userFavoriteRepository;
        this.movieRepository = movieRepository;

    }

    @Transactional 
    public PagedResponse<UserFavorite> addFavorite(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie with ID " + movieId + " not found."));

        UserMovieId id = new UserMovieId(userId, movieId);

        if (userFavoriteRepository.existsById(id)) {
            throw new IllegalArgumentException("Movie with ID " + movieId + " is already favorited by user " + userId + ".");
        }

        UserFavorite userFavorite = new UserFavorite(userId, movie);
        userFavorite.setFavoritedAt(LocalDateTime.now()); 
        UserFavorite savedFavorite = userFavoriteRepository.save(userFavorite);

        return new PagedResponse<>(
                Collections.singletonList(savedFavorite),
                0,
                1, 
                1L, 
                1,
                true 
        );
    }

    @Transactional
    public PagedResponse<UserFavorite> removeFavorite(Long userId, Long movieId) {
        UserMovieId id = new UserMovieId(userId, movieId);
        Optional<UserFavorite> favoriteOptional = userFavoriteRepository.findById(id);

        if (favoriteOptional.isEmpty()) {
            throw new IllegalArgumentException("Favorite for user " + userId + " and movie " + movieId + " not found.");
        }

        UserFavorite removedFavorite = favoriteOptional.get();
        userFavoriteRepository.deleteById(id);

        return new PagedResponse<>(
                Collections.singletonList(removedFavorite),
                0, 1, 1L, 1, true
        );
    }

    @Transactional(readOnly = true) 
    public PagedResponse<Boolean> isFavorited(Long userId, Long movieId) {
        UserMovieId id = new UserMovieId(userId, movieId);
        boolean isFav = userFavoriteRepository.existsById(id);

        return new PagedResponse<>(
                Collections.singletonList(isFav),
                0, 1, 1L, 1, true
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserFavorite> getFavoritesByUserId(Long userId, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<UserFavorite> userFavoritesPage = userFavoriteRepository.findById_UserId(userId, pageable);

        return new PagedResponse<>(
                userFavoritesPage.getContent(),
                userFavoritesPage.getNumber(),
                userFavoritesPage.getSize(),
                userFavoritesPage.getTotalElements(),
                userFavoritesPage.getTotalPages(),
                userFavoritesPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserFavorite> getFavorite(Long userId, Long movieId) {
        UserMovieId id = new UserMovieId(userId, movieId);
        Optional<UserFavorite> favoriteOptional = userFavoriteRepository.findById(id);

        return favoriteOptional.map(favorite -> new PagedResponse<>(
                Collections.singletonList(favorite),
                0, 1, 1L, 1, true
        )).orElseGet(() -> new PagedResponse<>(
                Collections.emptyList(),
                0, 0, 0L, 0, true
        ));
    }
}
