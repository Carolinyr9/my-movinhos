package br.ifsp.my_movinhos.controller;

import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.model.UserFavorite;
import br.ifsp.my_movinhos.service.UserFavoriteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable; 
import org.springframework.data.web.PageableDefault; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class UserFavoriteController {

    private final UserFavoriteService userFavoriteService;

    @Autowired
    public UserFavoriteController(UserFavoriteService userFavoriteService) {
        this.userFavoriteService = userFavoriteService;
    }

    @PostMapping("/{userId}/{movieId}")
    public ResponseEntity<PagedResponse<UserFavorite>> addFavorite(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        try {
            PagedResponse<UserFavorite> response = userFavoriteService.addFavorite(userId, movieId);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<PagedResponse<UserFavorite>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<PagedResponse<UserFavorite>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{userId}/{movieId}")
    public ResponseEntity<PagedResponse<UserFavorite>> removeFavorite(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        try {
            PagedResponse<UserFavorite> response = userFavoriteService.removeFavorite(userId, movieId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<PagedResponse<UserFavorite>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<PagedResponse<UserFavorite>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{userId}/{movieId}/exists")
    public ResponseEntity<PagedResponse<Boolean>> isFavorited(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        try {
            PagedResponse<Boolean> response = userFavoriteService.isFavorited(userId, movieId);
            return new ResponseEntity<>(response, HttpStatus.OK); 
        } catch (Exception e) {
            return new ResponseEntity<PagedResponse<Boolean>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PagedResponse<UserFavorite>> getFavoritesByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) { 
        try {
            PagedResponse<UserFavorite> favorites = userFavoriteService.getFavoritesByUserId(
                userId, pageable.getPageNumber(), pageable.getPageSize());
            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            return new ResponseEntity<PagedResponse<UserFavorite>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
    @GetMapping("/{userId}/{movieId}")
    public ResponseEntity<PagedResponse<UserFavorite>> getFavorite(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        try {
            PagedResponse<UserFavorite> response = userFavoriteService.getFavorite(userId, movieId);
            if (response.getContent().isEmpty()) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(response, HttpStatus.OK); 
        } catch (Exception e) {
            return new ResponseEntity<PagedResponse<UserFavorite>>(new PagedResponse<>(
                    null, 0, 0, 0L, 0, true), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}