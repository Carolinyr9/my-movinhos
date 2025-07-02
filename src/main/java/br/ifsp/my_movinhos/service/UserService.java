package br.ifsp.my_movinhos.service;

import java.util.Map;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ifsp.my_movinhos.dto.MovieResponseDTO;
import br.ifsp.my_movinhos.dto.RoleResponseDTO;
import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.exception.ResourceNotFoundException;
import br.ifsp.my_movinhos.external.auth.AuthServiceClient;
import br.ifsp.my_movinhos.mapper.PagedResponseMapper;
import br.ifsp.my_movinhos.model.Genre;
import br.ifsp.my_movinhos.model.Movie;
import br.ifsp.my_movinhos.model.Role;
import br.ifsp.my_movinhos.model.User;
import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.model.UserWatched;
import br.ifsp.my_movinhos.model.enums.RoleName;
import br.ifsp.my_movinhos.model.key.UserMovieId;
import br.ifsp.my_movinhos.repository.MovieRepository;
import br.ifsp.my_movinhos.repository.ReviewRepository;
import br.ifsp.my_movinhos.repository.RoleRepository;
import br.ifsp.my_movinhos.repository.UserFavoriteRepository;
import br.ifsp.my_movinhos.repository.UserRepository;
import br.ifsp.my_movinhos.repository.UserWatchedRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final UserWatchedRepository userWatchedRepository;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;
    private final AuthServiceClient authServiceClient; 

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       MovieRepository movieRepository,
                       UserFavoriteRepository userFavoriteRepository,
                       UserWatchedRepository userWatchedRepository,
                       ModelMapper modelMapper,
                       PagedResponseMapper pagedResponseMapper,
                       ReviewRepository reviewRepository,
                       AuthServiceClient authServiceClient) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.movieRepository = movieRepository;
        this.userFavoriteRepository = userFavoriteRepository;
        this.userWatchedRepository = userWatchedRepository;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
        this.reviewRepository = reviewRepository;
        this.authServiceClient = authServiceClient; 
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponseDTO> getAllUsersFromAuthService(Pageable pageable) {
        PagedResponse<UserResponseDTO> usersFromAuth = authServiceClient.getAllUsers(pageable); 

        List<UserResponseDTO> synchronizedUsers = usersFromAuth.getContent().stream()
                .map(this::syncUserFromAuthService) 
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

        return syncUserFromAuthService(userFromAuth);
    }


    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsernameFromAuthService(String username) {
        UserResponseDTO userFromAuth = authServiceClient.getUserByUsername(username); 
        return syncUserFromAuthService(userFromAuth);
    }

    @Transactional
    private UserResponseDTO syncUserFromAuthService(UserResponseDTO authUserDTO) {
        return userRepository.findById(authUserDTO.getId())
                .map(localUser -> {
                    localUser.setUsername(authUserDTO.getUsername());
                    localUser.setEmail(authUserDTO.getEmail());
                    localUser.setName(authUserDTO.getName());

                    updateLocalUserRoles(localUser, authUserDTO.getRoles()); 

                    userRepository.save(localUser); 
                    return modelMapper.map(localUser, UserResponseDTO.class);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setId(authUserDTO.getId()); 
                    newUser.setUsername(authUserDTO.getUsername());
                    newUser.setEmail(authUserDTO.getEmail());
                    newUser.setName(authUserDTO.getName());

                    Set<Role> roles = authUserDTO.getRoles().stream()
                    .map((RoleResponseDTO roleDTO) -> {
                        RoleName roleName = roleDTO.getRoleName();
                        return roleRepository.findByRoleName(roleName)
                                .orElseGet(() -> roleRepository.save(new Role(roleName)));
                    })
                    .collect(Collectors.toSet());


                    for (Role role : roles) {
                        newUser.addRole(role); 
                    }

                    // Senha não é gerenciada aqui
                    newUser.setPassword("NOT_APPLICABLE_VIA_MONOLITH");

                    User savedUser = userRepository.save(newUser);
                    return modelMapper.map(savedUser, UserResponseDTO.class);
                });
    }

    private void updateLocalUserRoles(User localUser, Set<RoleResponseDTO> authServiceRoles) {
        Set<Role> newRoles = authServiceRoles.stream()
                .map((RoleResponseDTO roleDTO) -> {
                    RoleName roleName = roleDTO.getRoleName();
                    return roleRepository.findByRoleName(roleName)
                            .orElseGet(() -> roleRepository.save(new Role(roleName)));
                })
                .collect(Collectors.toSet());

        for (Role role : newRoles) {
            localUser.addRole(role); // Certifique-se de que addRole evita duplicação
        }
    }

    @Transactional
    public void deleteUserLocalData(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local user data not found for id: " + id));
                
        reviewRepository.deleteByUserWatched_User_Id(id); 
        userFavoriteRepository.deleteByUserId(id); 
        userWatchedRepository.deleteByUserId(id);  

        userRepository.delete(user);
    }

    @Transactional
    public void addFavoriteMovie(Long userId, Long movieId) {
        UserMovieId favoriteId = new UserMovieId(userId, movieId);
        if (userFavoriteRepository.existsById(favoriteId)) {
            throw new IllegalArgumentException("Movie with id " + movieId + " is already a favorite for user with id " + userId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        UserFavorite favorite = new UserFavorite(user, movie);
        userFavoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavoriteMovie(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        user.removeFavorite(movie);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MovieResponseDTO> getFavoriteMovies(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        List<Movie> favoriteMovies = userRepository.findById(userId).get().getFavoriteMovies().stream()
                .map(favorite -> favorite.getMovie())
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), favoriteMovies.size());
        
        Page<Movie> page = new PageImpl<>(favoriteMovies.subList(start, end), pageable, favoriteMovies.size());

        return pagedResponseMapper.toPagedResponse(page, MovieResponseDTO.class);
    }

    @Transactional
    public void addWatchedMovie(Long userId, Long movieId) {
        UserMovieId watchedId = new UserMovieId(userId, movieId);
        if (userWatchedRepository.existsById(watchedId)) {
            throw new IllegalArgumentException("Movie with id " + movieId + " is already marked as watched for user with id " + userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));
        
        UserWatched watched = new UserWatched(user, movie, LocalDateTime.now());
        userWatchedRepository.save(watched);
    }

    @Transactional
    public void removeWatchedMovie(Long userId, Long movieId) {
        UserMovieId watchedId = new UserMovieId(userId, movieId);
        if (userWatchedRepository.existsById(watchedId)) {
            userWatchedRepository.deleteById(watchedId);
            
            reviewRepository.deleteByUserWatchedMovieId(movieId); 
        } else {
            throw new ResourceNotFoundException("Watched record not found for user " + userId + " and movie " + movieId);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<MovieResponseDTO> getWatchedMovies(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        User user = userRepository.findById(userId).get();
        
        List<Movie> watchedMoviesList = user.getWatchedMovies().stream()
                .map(UserWatched::getMovie)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), watchedMoviesList.size());
        
        Page<Movie> pageResult = new PageImpl<>(watchedMoviesList.subList(start, end), pageable, watchedMoviesList.size());

        return pagedResponseMapper.toPagedResponse(pageResult, MovieResponseDTO.class);
    }

    public List<Genre> getTopGenresForUser(Long userId, int limit) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        User user = userRepository.findById(userId).get();

        List<Movie> watched = user.getWatchedMovies().stream()
            .map(UserWatched::getMovie).toList();
        
        List<Movie> favorites = user.getFavoriteMovies().stream()
            .map(UserFavorite::getMovie).toList();

        List<Movie> all = Stream.concat(watched.stream(), favorites.stream()).toList();

        Map<Genre, Long> genreFrequency = all.stream()
            .flatMap(movie -> movie.getGenres().stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return genreFrequency.entrySet().stream()
            .sorted(Map.Entry.<Genre, Long>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
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
