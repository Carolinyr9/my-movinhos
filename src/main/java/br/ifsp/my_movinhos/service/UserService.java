package br.ifsp.my_movinhos.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ifsp.my_movinhos.dto.MovieResponseDTO;
import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.exception.ResourceNotFoundException;
import br.ifsp.my_movinhos.external.auth.AuthServiceClient;
import br.ifsp.my_movinhos.mapper.PagedResponseMapper;
import br.ifsp.my_movinhos.model.Genre;
import br.ifsp.my_movinhos.model.Movie;
import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.model.UserWatched;
import br.ifsp.my_movinhos.repository.MovieRepository;
import br.ifsp.my_movinhos.repository.UserWatchedRepository;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService {
    private final MovieRepository movieRepository;
    private final UserFavoriteService userFavoriteService;
    private final UserWatchedRepository userWatchedRepository;
    private final PagedResponseMapper pagedResponseMapper;
    private final AuthServiceClient authServiceClient; 

    public UserService(
                       MovieRepository movieRepository,
                       UserFavoriteService userFavoriteService,
                       UserWatchedRepository userWatchedRepository,
                       PagedResponseMapper pagedResponseMapper,
                       AuthServiceClient authServiceClient) {
        this.movieRepository = movieRepository;
        this.userFavoriteService = userFavoriteService;
        this.userWatchedRepository = userWatchedRepository;
        this.pagedResponseMapper = pagedResponseMapper;
        this.authServiceClient = authServiceClient; 
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponseDTO> getAllUsersFromAuthService(Pageable pageable) {
        PagedResponse<UserResponseDTO> usersFromAuth = authServiceClient.getAllUsers(pageable); 

        List<UserResponseDTO> synchronizedUsers = usersFromAuth.getContent().stream()
                .collect(Collectors.toList());

        return new PagedResponse<>(
                synchronizedUsers,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                usersFromAuth.getTotalElements(),
                usersFromAuth.getTotalPages(),
                usersFromAuth.isLast()
        );
    }

 
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByIdFromAuthService(Long id) {
        UserResponseDTO userFromAuth = authServiceClient.getUserById(id); 

        return userFromAuth;
    }


    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsernameFromAuthService(String username) {
        UserResponseDTO userFromAuth = authServiceClient.getUserByUsername(username); 
        return userFromAuth;
    }

    @Transactional(readOnly = true) // Apenas leitura de dados
    public List<Genre> getTopGenresForUser(Long userId, int limit) {
        if(getUserByIdFromAuthService(userId) == null) {
            new ResourceNotFoundException("Usuário não encontrado");
        } 

        List<UserWatched> watchedEntries = userWatchedRepository.findById_UserId(userId);
        List<Movie> watchedMovies = watchedEntries.stream()
                .map(UserWatched::getMovie)
                .collect(Collectors.toList());

        PagedResponse<UserFavorite> favoriteEntriesPage = userFavoriteService.getFavoritesByUserId(userId, 0, Integer.MAX_VALUE); 
        List<Movie> favoriteMovies = favoriteEntriesPage.getContent().stream()
                .map(UserFavorite::getMovie)
                .collect(Collectors.toList());

        List<Movie> allMovies = Stream.concat(watchedMovies.stream(), favoriteMovies.stream())
                .collect(Collectors.toList());

        Map<Genre, Long> genreFrequency = allMovies.stream()
                .flatMap(movie -> movie.getGenres().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())); 

        return genreFrequency.entrySet().stream()
                .sorted(Map.Entry.<Genre, Long>comparingByValue().reversed()) 
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public PagedResponse<MovieResponseDTO> getRecommendedMoviesByGenres(List<Genre> genres, Pageable pageable){
        if (genres == null || genres.isEmpty()) {
            throw new IllegalArgumentException("Genres list cannot be null or empty.");
        }

        List<Movie> recommendedMovies = movieRepository.findByGenresContaining(genres.get(0), pageable)
                .stream()
                .filter(movie -> movie.getGenres().stream().anyMatch(genres::contains))
                .collect(Collectors.toList());

        Page<Movie> moviePage = new PageImpl<>(recommendedMovies, pageable, recommendedMovies.size());
        return pagedResponseMapper.toPagedResponse(moviePage, MovieResponseDTO.class);
    }
}
