package br.ifsp.my_movinhos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import br.ifsp.my_movinhos.dto.MovieResponseDTO;
import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.dto.GenreResponseDTO;
import br.ifsp.my_movinhos.exception.ErrorResponse;
import br.ifsp.my_movinhos.model.Genre;
import br.ifsp.my_movinhos.model.Movie;
import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.model.UserWatched;
import br.ifsp.my_movinhos.service.UserFavoriteService;
import br.ifsp.my_movinhos.service.UserService;
import br.ifsp.my_movinhos.service.UserWatchedService;
import org.springframework.security.core.Authentication;


@Tag(name = "Usuários", description = "Gerenciamento de usuários")
@Validated
@RestController
@RequestMapping("/my-movinhos/users")
public class UserController {
    private final UserFavoriteService userFavoriteService;
    private final UserService userService;
    private final UserWatchedService userWatchedService;

    public UserController(UserService userService, UserFavoriteService userFavoriteService, UserWatchedService userWatchedService) {
        this.userService = userService;
        this.userFavoriteService = userFavoriteService;
        this.userWatchedService = userWatchedService;
    }

    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista paginada de todos os usuários. Os dados dos usuários são obtidos do microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários recuperada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<PagedResponse<UserResponseDTO>> getAllUsers(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        PagedResponse<UserResponseDTO> users = userService.getAllUsersFromAuthService(pageable);
        return ResponseEntity.ok(users);
    }

    
    @GetMapping("/me") 
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<UserResponseDTO> getAuthenticatedUser(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());

        UserResponseDTO user = userService.getUserByIdFromAuthService(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna um único usuário pelo seu ID exclusivo. Os dados do usuário são obtidos do microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário recuperado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #id)")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserByIdFromAuthService(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Buscar usuário por nome de usuário (username)", description = "Retorna um único usuário pelo seu nome de usuário (username) exclusivo. Os dados do usuário são obtidos do microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário recuperado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search/by-username")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #username)")
    public ResponseEntity<UserResponseDTO> getUserByUsername(
        @RequestParam @NotBlank(message = "Username cannot be blank") String username
    ) {
        UserResponseDTO user = userService.getUserByUsernameFromAuthService(username);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Adicionar um filme aos favoritos do usuário", description = "Adiciona um filme à lista de favoritos de um usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme favoritado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping("/{userId}/favorites/{movieId}")
    public ResponseEntity<Void> addFavoriteMovie(@PathVariable Long userId, @PathVariable Long movieId) {
        userFavoriteService.addFavorite(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remover um filme dos favoritos do usuário", description = "Remove um filme da lista de favoritos de um usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme removido dos favoritos com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{userId}/favorites/{movieId}")
    public ResponseEntity<Void> removeFavoriteMovie(@PathVariable Long userId, @PathVariable Long movieId) {
        userFavoriteService.removeFavorite(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar filmes favoritos do usuário", description = "Retorna uma lista paginada dos filmes favoritos de um usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de filmes favoritos recuperada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{userId}/favorites")
    public ResponseEntity<PagedResponse<MovieResponseDTO>> getFavoriteMovies(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) { 

        PagedResponse<UserFavorite> favoriteUserMovies = userFavoriteService.getFavoritesByUserId(
                userId, pageable.getPageNumber(), pageable.getPageSize());

       List<MovieResponseDTO> movieResponseDTOs = favoriteUserMovies.getContent().stream()
                .map(userFavorite -> {
                    Movie movie = userFavorite.getMovie();
                    MovieResponseDTO dto = new MovieResponseDTO();
                    dto.setId(movie.getId());
                    dto.setTitle(movie.getTitle());
                    dto.setGenres(movie.getGenres().stream()
                            .map(genre -> new GenreResponseDTO(genre.getId(), genre.getName()))
                            .collect(Collectors.toSet()));
                
                    return dto;
                })
                .collect(Collectors.toList());

        PagedResponse<MovieResponseDTO> pagedMovieResponseDTO = new PagedResponse<>(
                movieResponseDTOs,
                favoriteUserMovies.getPage(),
                favoriteUserMovies.getSize(),
                favoriteUserMovies.getTotalElements(),
                favoriteUserMovies.getTotalPages(),
                favoriteUserMovies.isLast()
        );

        return ResponseEntity.ok(pagedMovieResponseDTO);
    }

    @Operation(summary = "Marcar um filme como assistido pelo usuário", description = "Marca um filme como assistido para um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme marcado como assistido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping("/{userId}/watched/{movieId}")
    public ResponseEntity<Void> markMovieAsWatched(@PathVariable Long userId, @PathVariable Long movieId) {
        userWatchedService.addWatchedMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desmarcar um filme como assistido pelo usuário", description = "Remove a marcação de assistido de um filme para um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme desmarcado como assistido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Registro de 'assistido' não encontrado, ou usuário/filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{userId}/watched/{movieId}")
    public ResponseEntity<Void> unmarkMovieAsWatched(@PathVariable Long userId, @PathVariable Long movieId) {
        userWatchedService.removeWatchedMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar filmes assistidos pelo usuário", description = "Retorna uma lista paginada dos filmes que um usuário marcou como assistidos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de filmes assistidos recuperada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{userId}/watched")
    public ResponseEntity<PagedResponse<MovieResponseDTO>> getWatchedMovies(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "watchedAt") Pageable pageable) {
        PagedResponse<UserWatched> watchedMovies = userWatchedService.getWatchedByUserId(userId, pageable);

        List<MovieResponseDTO> movieResponseDTOs = watchedMovies.getContent().stream()
                .map(userFavorite -> {
                    Movie movie = userFavorite.getMovie();
                    MovieResponseDTO dto = new MovieResponseDTO();
                    dto.setId(movie.getId());
                    dto.setTitle(movie.getTitle());
                    dto.setGenres(movie.getGenres().stream()
                            .map(genre -> new GenreResponseDTO(genre.getId(), genre.getName()))
                            .collect(Collectors.toSet()));
                
                    return dto;
                })
                .collect(Collectors.toList());

        PagedResponse<MovieResponseDTO> pagedMovieResponseDTO = new PagedResponse<>(
                movieResponseDTOs,
                watchedMovies.getPage(),
                watchedMovies.getSize(),
                watchedMovies.getTotalElements(),
                watchedMovies.getTotalPages(),
                watchedMovies.isLast()
        );

        return ResponseEntity.ok(pagedMovieResponseDTO);
    }


    @Operation(summary = "Listar filmes recomendados para o usuário", description = "Retorna uma lista paginada dos filmes recomendados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de filmes recomendados"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "400", description = "Gêneros não encontrados ou inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{userId}/recommendations")
    public ResponseEntity<PagedResponse<MovieResponseDTO>> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        List<Genre> topGenres = userService.getTopGenresForUser(userId, 3);
        PagedResponse<MovieResponseDTO> recommendedMovies = userService.getRecommendedMoviesByGenres(topGenres, pageable);
        return ResponseEntity.ok(recommendedMovies);
    }
}