package br.ifsp.my_movinhos.model;

import jakarta.persistence.*;
import lombok.*;

import br.ifsp.my_movinhos.model.common.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@EqualsAndHashCode(of = "id") 
@Table(name = "roles")
public class Role extends BaseEntity { 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    public Role(String roleName) {
        this.roleName = roleName;
    }
}


