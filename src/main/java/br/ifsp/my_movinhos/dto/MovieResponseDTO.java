package br.ifsp.my_movinhos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Set;

import br.ifsp.my_movinhos.model.enums.ContentRating;

import java.util.HashSet;
import lombok.Builder;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieResponseDTO {
    private Long id;
    private String title;
    private String synopsis;
    private int releaseYear;
    private int duration;
    private ContentRating contentRating;
    private Set<GenreResponseDTO> genres = new HashSet<>();
}