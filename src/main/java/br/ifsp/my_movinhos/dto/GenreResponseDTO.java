package br.ifsp.my_movinhos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenreResponseDTO {
    private Long id;
    private String name;
}