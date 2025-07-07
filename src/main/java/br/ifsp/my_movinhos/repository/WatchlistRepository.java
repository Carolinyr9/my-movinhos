package br.ifsp.my_movinhos.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.ifsp.my_movinhos.model.Watchlist;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    Page<Watchlist> findAllByUserId(Long userId, Pageable pageable);

    Optional<Watchlist> findByIdAndUserId(Long id, Long userId);
}
