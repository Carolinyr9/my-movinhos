package br.ifsp.my_movinhos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.ifsp.my_movinhos.model.Role;
import br.ifsp.my_movinhos.model.enums.RoleName;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(RoleName roleName);
}
