package br.ifsp.my_movinhos.dto;

import br.ifsp.my_movinhos.model.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDTO {

    private Long id;

    private RoleName roleName;
}
