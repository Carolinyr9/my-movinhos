package br.ifsp.my_movinhos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ifsp.my_movinhos.model.ContentFlag;
import br.ifsp.my_movinhos.model.key.UserReviewId;

public interface ContentFlagRepository extends JpaRepository<ContentFlag, UserReviewId> {
    boolean existsById(UserReviewId id);

}
