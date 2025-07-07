package br.ifsp.my_movinhos.model.key;

import jakarta.persistence.Column; // Importar Column
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Embeddable 
public class UserMovieId implements Serializable {
    private static final long serialVersionUID = 1L; 

    @Column(name = "user_id") 
    private Long userId;

    @Column(name = "movie_id")
    private Long movieId;
}
