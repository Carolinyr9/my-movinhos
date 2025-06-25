package br.ifsp.my_movinhos.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.ifsp.my_movinhos.model.Watchlist;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    Page<Watchlist> findAllByUser_Id(Long id, Pageable pageable);

    @Query("SELECT w FROM Watchlist w LEFT JOIN FETCH w.movies WHERE w.id = :watchlistId AND w.user.id = :userId")
    Optional<Watchlist> findByIdAndUserId(@Param("watchlistId") Long watchlistId, @Param("userId") Long userId);
}
