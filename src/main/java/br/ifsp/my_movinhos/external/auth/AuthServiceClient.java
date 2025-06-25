package br.ifsp.my_movinhos.external.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable; 
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import br.ifsp.my_movinhos.dto.UserPatchDTO;
import br.ifsp.my_movinhos.dto.UserRequestDTO;
import br.ifsp.my_movinhos.dto.UserRequestWithRolesDTO;
import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse; 

/**
 * Cliente para se comunicar com o microsserviço de autenticação.
 * Este cliente gerencia tanto o registro de usuários quanto as operações de CRUD completo.
 */
@Component
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;
    public AuthServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserResponseDTO registerUser(UserRequestDTO userRequestDTO) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserRequestDTO> request = new HttpEntity<>(userRequestDTO, headers);

        try {
            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                authServiceUrl + "/api/login", 
                HttpMethod.POST,
                request,
                UserResponseDTO.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Erro ao registrar usuário no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao registrar usuário: " + ex.getMessage(), ex);
        }
    }


    public PagedResponse<UserResponseDTO> getAllUsers(Pageable pageable) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authServiceUrl + "/api/users")
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize())
                .queryParam("sort", pageable.getSort().toString().replace(": ", ",")); // Formato "campo,direção"

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<PagedResponse> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                request,
                PagedResponse.class 
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Erro ao buscar todos os usuários no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao buscar todos os usuários: " + ex.getMessage(), ex);
        }
    }

    public UserResponseDTO getUserById(Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
            throw new RuntimeException("Erro ao buscar usuário por ID " + id + " no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao buscar usuário por ID: " + ex.getMessage(), ex);
        }
    }

    public UserResponseDTO getUserByUsername(String username) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authServiceUrl + "/api/users/search/by-username")
                .queryParam("username", username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
            throw new RuntimeException("Erro ao buscar usuário por username '" + username + "' no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao buscar usuário por username: " + ex.getMessage(), ex);
        }
    }

    public UserResponseDTO createUserByAdmin(UserRequestWithRolesDTO userRequestWithRolesDTO) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserRequestWithRolesDTO> request = new HttpEntity<>(userRequestWithRolesDTO, headers);

        try {
            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                authServiceUrl + "/api/users",
                HttpMethod.POST,
                request,
                UserResponseDTO.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Erro ao criar usuário por admin no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao criar usuário por admin: " + ex.getMessage(), ex);
        }
    }

    public UserResponseDTO updateUser(Long id, UserRequestWithRolesDTO userRequestDTO) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserRequestWithRolesDTO> request = new HttpEntity<>(userRequestDTO, headers);

        try {
            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                authServiceUrl + "/api/users/" + id,
                HttpMethod.PUT,
                request,
                UserResponseDTO.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Erro ao atualizar usuário ID " + id + " no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao atualizar usuário: " + ex.getMessage(), ex);
        }
    }

    public UserResponseDTO patchUser(Long id, UserPatchDTO userPatchDTO) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserPatchDTO> request = new HttpEntity<>(userPatchDTO, headers);

        try {
            ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                authServiceUrl + "/api/users/" + id,
                HttpMethod.PATCH,
                request,
                UserResponseDTO.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Erro ao fazer PATCH no usuário ID " + id + " no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao fazer PATCH no usuário: " + ex.getMessage(), ex);
        }
    }

    public void deleteUser(Long id) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                authServiceUrl + "/api/users/" + id,
                HttpMethod.DELETE,
                request,
                Void.class
            );
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Erro ao excluir usuário ID " + id + " no microsserviço de autenticação: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erro inesperado ao excluir usuário: " + ex.getMessage(), ex);
        }
    }
}