package br.ifsp.my_movinhos.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

import br.ifsp.my_movinhos.model.common.BaseEntity;
import br.ifsp.my_movinhos.model.enums.RoleName;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20, name = "role_name")
    private RoleName roleName;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.EAGER)
    private Set<User> users = new HashSet<>();

    public Role(RoleName roleName) {
        this.roleName = roleName;
    }
}


