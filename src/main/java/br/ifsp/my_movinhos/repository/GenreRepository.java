package br.ifsp.my_movinhos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ifsp.my_movinhos.model.Genre;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByNameIgnoreCase(String name);
    
}
