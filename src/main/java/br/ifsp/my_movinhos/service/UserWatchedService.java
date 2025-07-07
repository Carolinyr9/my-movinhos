package br.ifsp.my_movinhos.service;

import br.ifsp.my_movinhos.model.Movie;
import br.ifsp.my_movinhos.model.UserWatched;
import br.ifsp.my_movinhos.model.key.UserMovieId;
import br.ifsp.my_movinhos.repository.MovieRepository;
import br.ifsp.my_movinhos.repository.UserWatchedRepository;
import br.ifsp.my_movinhos.dto.page.PagedResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service 
public class UserWatchedService {

    private final UserWatchedRepository userWatchedRepository;
    private final MovieRepository movieRepository; 

    @Autowired
    public UserWatchedService(UserWatchedRepository userWatchedRepository, MovieRepository movieRepository) {
        this.userWatchedRepository = userWatchedRepository;
        this.movieRepository = movieRepository;
    }

    @Transactional
    public UserWatched addWatchedMovie(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie with ID " + movieId + " not found."));

        UserMovieId id = new UserMovieId(userId, movieId);

        if (userWatchedRepository.existsById(id)) {
            throw new IllegalArgumentException("Movie with ID " + movieId + " is already marked as watched by user " + userId + ".");
        }

        UserWatched userWatched = new UserWatched(userId, movie, LocalDateTime.now());

        return userWatchedRepository.save(userWatched);
    }

    @Transactional
    public void removeWatchedMovie(Long userId, Long movieId) {
        UserMovieId id = new UserMovieId(userId, movieId);

        if (!userWatchedRepository.existsById(id)) {
            throw new IllegalArgumentException("Watched entry for user " + userId + " and movie " + movieId + " not found.");
        }

        userWatchedRepository.deleteById(id);
    }

    
    @Transactional(readOnly = true)
    public PagedResponse<UserWatched> getWatchedByUserId(Long userId, Pageable pageable) {
        Page<UserWatched> userWatchedPage = userWatchedRepository.findById_UserId(userId, pageable);
        return new PagedResponse<>(
                userWatchedPage.getContent(),
                userWatchedPage.getNumber(),
                userWatchedPage.getSize(),
                userWatchedPage.getTotalElements(),
                userWatchedPage.getTotalPages(),
                userWatchedPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public Optional<UserWatched> getWatchedEntry(Long userId, Long movieId) {
        UserMovieId id = new UserMovieId(userId, movieId);
        return userWatchedRepository.findById(id);
    }


    @Transactional
    public UserWatched addReviewToWatched(Long userId, Long movieId, String content,
                                          int directionScore, int screenplayScore,
                                          int cinematographyScore, int generalScore) {
        UserMovieId id = new UserMovieId(userId, movieId);
        UserWatched userWatched = userWatchedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Watched entry for user " + userId + " and movie " + movieId + " not found. Cannot add review."));

        userWatched.addReview(content, directionScore, screenplayScore, cinematographyScore, generalScore);

        return userWatchedRepository.save(userWatched);
    }

    @Transactional
    public UserWatched removeReviewFromWatched(Long userId, Long movieId) {
        UserMovieId id = new UserMovieId(userId, movieId);
        UserWatched userWatched = userWatchedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Watched entry for user " + userId + " and movie " + movieId + " not found. Cannot remove review."));

        if (userWatched.getReview() == null) {
            throw new IllegalArgumentException("No review found for watched entry by user " + userId + " and movie " + movieId + ".");
        }

        userWatched.removeReview();
        return userWatchedRepository.save(userWatched);
    }
}
