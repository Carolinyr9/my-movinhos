package br.ifsp.my_movinhos.external.auth;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse;

/**
 * Cliente para se comunicar com o microsserviço de autenticação.
 * Este cliente é responsável apenas por buscar informações de usuários.
 * Operações de registro, atualização e exclusão são de responsabilidade exclusiva do microsserviço de autenticação.
 */
@Component
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public AuthServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Adiciona o cabeçalho de autorização (Bearer Token) à requisição HTTP,
     * obtendo o token do contexto de segurança atual.
     *
     * @param headers Os cabeçalhos HTTP aos quais o token será adicionado.
     */
    private void addAuthorizationHeader(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            headers.setBearerAuth(jwt.getTokenValue());
        }
        // Se o principal não for um Jwt (por exemplo, em testes ou se a autenticação for diferente),
        // ou se não houver autenticação, o cabeçalho não será adicionado.
        // Isso é esperado se o endpoint do microsserviço permitir acesso não autenticado ou
        // se a lógica de chamada garantir que o token já foi validado.
    }

    /**
     * Busca todos os usuários paginados do microsserviço de autenticação.
     * Utiliza ParameterizedTypeReference para lidar corretamente com o tipo genérico PagedResponse<UserResponseDTO>.
     *
     * @param pageable Informações de paginação.
     * @return Uma PagedResponse contendo uma lista de UserResponseDTOs.
     * @throws RuntimeException se ocorrer um erro durante a comunicação com o microsserviço.
     */
    public PagedResponse<UserResponseDTO> getAllUsers(Pageable pageable) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authServiceUrl + "/api/users")
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            String sortParams = pageable.getSort().stream()
                    .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                    .collect(Collectors.joining("&sort="));
            builder.queryParam("sort", sortParams);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthorizationHeader(headers); // Adiciona o cabeçalho de autorização
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<PagedResponse<UserResponseDTO>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<PagedResponse<UserResponseDTO>>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            String errorMessage = "Erro ao buscar todos os usuários no microsserviço de autenticação (Status: " + ex.getStatusCode() + ")";
            if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
                errorMessage += ": " + ex.getResponseBodyAsString();
            } else {
                errorMessage += ". Resposta vazia ou ilegível.";
            }
            throw new RuntimeException(errorMessage, ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao buscar todos os usuários: " + ex.getMessage(), ex);
        }
    }

    /**
     * Busca um usuário por ID no microsserviço de autenticação.
     *
     * @param id O ID do usuário.
     * @return O UserResponseDTO correspondente ao ID.
     * @throws RuntimeException se o usuário não for encontrado ou ocorrer um erro.
     */
    public UserResponseDTO getUserById(Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthorizationHeader(headers); // Adiciona o cabeçalho de autorização
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                authServiceUrl + "/api/users/" + id,
                HttpMethod.GET,
                request,
                UserResponseDTO.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            String errorMessage = "Erro ao buscar usuário por ID " + id + " no microsserviço de autenticação (Status: " + ex.getStatusCode() + ")";
            if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
                errorMessage += ": " + ex.getResponseBodyAsString();
            } else {
                errorMessage += ". Resposta vazia ou ilegível.";
            }
            throw new RuntimeException(errorMessage, ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao buscar usuário por ID: " + ex.getMessage(), ex);
        }
    }

    /**
     * Busca um usuário por nome de usuário no microsserviço de autenticação.
     *
     * @param username O nome de usuário.
     * @return O UserResponseDTO correspondente ao nome de usuário.
     * @throws RuntimeException se o usuário não for encontrado ou ocorrer um erro.
     */
    public UserResponseDTO getUserByUsername(String username) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authServiceUrl + "/api/users/search/by-username")
                .queryParam("username", username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthorizationHeader(headers); // Adiciona o cabeçalho de autorização
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                request,
                UserResponseDTO.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            String errorMessage = "Erro ao buscar usuário por username '" + username + "' no microsserviço de autenticação (Status: " + ex.getStatusCode() + ")";
            if (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isEmpty()) {
                errorMessage += ": " + ex.getResponseBodyAsString();
            } else {
                errorMessage += ". Resposta vazia ou ilegível.";
            }
            throw new RuntimeException(errorMessage, ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao buscar usuário por username: " + ex.getMessage(), ex);
        }
    }
}
