package br.ifsp.my_movinhos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import br.ifsp.my_movinhos.dto.MovieResponseDTO;
import br.ifsp.my_movinhos.dto.UserPatchDTO;
import br.ifsp.my_movinhos.dto.UserRequestDTO;
import br.ifsp.my_movinhos.dto.UserRequestWithRolesDTO;
import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.exception.ErrorResponse;
import br.ifsp.my_movinhos.external.auth.AuthServiceClient;
import br.ifsp.my_movinhos.model.Genre;
import br.ifsp.my_movinhos.repository.MovieRepository;
import br.ifsp.my_movinhos.service.UserService;


@Tag(name = "Usuários", description = "Gerenciamento de usuários")
@Validated
@RestController
@RequestMapping("/my-movinhos/users")
public class UserController {

    private final UserService userService;
    private final MovieRepository movieRepository; // Mantido, mas o UserService deve ser o principal a usar o repo
    private final AuthServiceClient authServiceClient;

    public UserController(UserService userService, MovieRepository movieRepository, AuthServiceClient authServiceClient) {
        this.userService = userService;
        this.movieRepository = movieRepository;
        this.authServiceClient = authServiceClient;
    }

    @Operation(summary = "Listar todos os usuários", description = "Retorna uma lista paginada de todos os usuários. Os dados dos usuários são obtidos do microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários recuperada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Geralmente, listar todos os usuários é uma ação de ADMIN
    public ResponseEntity<PagedResponse<UserResponseDTO>> getAllUsers(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        // O UserService agora deve delegar a chamada para o authServiceClient para obter a lista de usuários
        // e, se necessário, enriquecer com dados locais.
        PagedResponse<UserResponseDTO> users = userService.getAllUsersFromAuthService(pageable);
        return ResponseEntity.ok(users);
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
        // O UserService agora deve delegar a chamada para o authServiceClient para obter os detalhes do usuário.
        UserResponseDTO user = userService.getUserByUsernameFromAuthService(username);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Registrar um novo usuário (público)", description = "Permite que qualquer visitante se registre. A criação do usuário é delegada ao microsserviço de autenticação. O usuário receberá o papel 'ROLE_USER' por padrão.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos / Erro de validação (ex: username/email já existe)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        // A criação do usuário principal agora é responsabilidade exclusiva do microsserviço de autenticação.
        // O monolito registra o usuário no auth service e então, se necessário, cria uma representação local
        // mínima para associar dados do catálogo de filmes, usando o ID retornado pelo auth service.
        UserResponseDTO createdUserInAuthService = authServiceClient.registerUser(userRequestDTO);

        // Opcional: Se o UserService ainda precisar de uma entrada local para o usuário (ex: para gerenciar filmes favoritos/assistidos),
        // ele pode criar uma entrada mínima aqui, usando o ID do usuário retornado pelo microsserviço de autenticação.
        // Por exemplo: userService.createLocalUserEntry(createdUserInAuthService.getId());
        // Certifique-se de que o UserResponseDTO do auth service contém o ID.

        return new ResponseEntity<>(createdUserInAuthService, HttpStatus.CREATED);
    }

    @Operation(summary = "Criar um novo usuário (Admin)", description = "Permite que um administrador crie um novo usuário, podendo especificar papéis. A criação é delegada ao microsserviço de autenticação. Requer perfil de ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso pelo administrador"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos / Erro de validação (ex: username/email já existe, ID de role inválido)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Requer perfil de ADMIN)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUserByAdmin(@Valid @RequestBody UserRequestWithRolesDTO userRequestWithRolesDTO) {
        // A criação do usuário e atribuição de roles é delegada ao microsserviço de autenticação.
        UserResponseDTO createdUserInAuthService = authServiceClient.createUserByAdmin(userRequestWithRolesDTO);

        // Opcional: Se o UserService ainda precisar de uma entrada local para o usuário,
        // ele pode criar uma entrada mínima aqui.
        // Por exemplo: userService.createLocalUserEntry(createdUserInAuthService.getId());

        return new ResponseEntity<>(createdUserInAuthService, HttpStatus.CREATED);
    }

    @Operation(summary = "Atualizar um usuário existente (atualização total)", description = "Atualiza todos os campos de um usuário existente pelo seu ID. A atualização é delegada ao microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos / Erro de validação",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Assume que apenas ADMIN pode fazer PUT total
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestWithRolesDTO userRequestDTO) {
        // A atualização principal do usuário é delegada ao microsserviço de autenticação.
        UserResponseDTO updatedUserInAuthService = authServiceClient.updateUser(id, userRequestDTO);

        // Opcional: Se houver dados de usuário localizados no monolito que precisam ser sincronizados,
        // o UserService pode lidar com isso aqui.
        // Por exemplo: userService.updateLocalUserEntry(id, updatedUserInAuthService);

        return ResponseEntity.ok(updatedUserInAuthService);
    }

    @Operation(summary = "Atualizar parcialmente um usuário existente", description = "Atualiza parcialmente campos de um usuário existente pelo seu ID usando semântica PATCH. A atualização é delegada ao microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário parcialmente atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos / Erro de validação",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #id)")
    public ResponseEntity<UserResponseDTO> patchUser(
            @PathVariable Long id,
            @Valid @RequestBody UserPatchDTO userPatchDTO) {
        // A atualização parcial do usuário é delegada ao microsserviço de autenticação.
        UserResponseDTO updatedUserInAuthService = authServiceClient.patchUser(id, userPatchDTO);

        // Opcional: Se houver dados de usuário localizados no monolito que precisam ser sincronizados,
        // o UserService pode lidar com isso aqui.
        // Por exemplo: userService.patchLocalUserEntry(id, updatedUserInAuthService);

        return ResponseEntity.ok(updatedUserInAuthService);
    }


    @Operation(summary = "Excluir um usuário", description = "Exclui um usuário pelo seu ID. A exclusão é delegada ao microsserviço de autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // Primeiro, excluímos o usuário no microsserviço de autenticação.
        authServiceClient.deleteUser(id);

        // Em seguida, o UserService do monolito é responsável por limpar quaisquer dados locais
        // associados a este usuário (como filmes favoritos/assistidos).
        userService.deleteUserLocalData(id);
        
        return ResponseEntity.noContent().build();
    }

    // --- Favorite Movies Endpoints ---
    @Operation(summary = "Adicionar um filme aos favoritos do usuário", description = "Adiciona um filme à lista de favoritos de um usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme favoritado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping("/{userId}/favorites/{movieId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #userId)")
    public ResponseEntity<Void> addFavoriteMovie(@PathVariable Long userId, @PathVariable Long movieId) {
        userService.addFavoriteMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remover um filme dos favoritos do usuário", description = "Remove um filme da lista de favoritos de um usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme removido dos favoritos com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{userId}/favorites/{movieId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #userId)")
    public ResponseEntity<Void> removeFavoriteMovie(@PathVariable Long userId, @PathVariable Long movieId) {
        userService.removeFavoriteMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar filmes favoritos do usuário", description = "Retorna uma lista paginada dos filmes favoritos de um usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de filmes favoritos recuperada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{userId}/favorites")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #userId)")
    public ResponseEntity<PagedResponse<MovieResponseDTO>> getFavoriteMovies(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        PagedResponse<MovieResponseDTO> favoriteMovies = userService.getFavoriteMovies(userId, pageable);
        return ResponseEntity.ok(favoriteMovies);
    }

    // --- Watched Movies Endpoints ---
    @Operation(summary = "Marcar um filme como assistido pelo usuário", description = "Marca um filme como assistido para um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme marcado como assistido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping("/{userId}/watched/{movieId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #userId)")
    public ResponseEntity<Void> markMovieAsWatched(@PathVariable Long userId, @PathVariable Long movieId) {
        userService.addWatchedMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desmarcar um filme como assistido pelo usuário", description = "Remove a marcação de assistido de um filme para um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme desmarcado como assistido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Registro de 'assistido' não encontrado, ou usuário/filme não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{userId}/watched/{movieId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #userId)")
    public ResponseEntity<Void> unmarkMovieAsWatched(@PathVariable Long userId, @PathVariable Long movieId) {
        userService.removeWatchedMovie(userId, movieId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar filmes assistidos pelo usuário", description = "Retorna uma lista paginada dos filmes que um usuário marcou como assistidos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de filmes assistidos recuperada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{userId}/watched")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(authentication, #userId)")
    public ResponseEntity<PagedResponse<MovieResponseDTO>> getWatchedMovies(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        PagedResponse<MovieResponseDTO> watchedMovies = userService.getWatchedMovies(userId, pageable);
        return ResponseEntity.ok(watchedMovies);
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