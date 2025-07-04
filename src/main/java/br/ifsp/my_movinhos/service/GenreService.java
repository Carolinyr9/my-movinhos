package br.ifsp.my_movinhos.service;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ifsp.my_movinhos.dto.GenreRequestDTO;
import br.ifsp.my_movinhos.dto.GenreResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.exception.ResourceNotFoundException;
import br.ifsp.my_movinhos.mapper.PagedResponseMapper;
import br.ifsp.my_movinhos.model.Genre;
import br.ifsp.my_movinhos.repository.GenreRepository;
import br.ifsp.my_movinhos.repository.MovieRepository;

@Service
public class GenreService {

    private GenreRepository genreRepository;
    private MovieRepository movieRepository;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public GenreService(GenreRepository genreRepository, MovieRepository movieRepository,
                        ModelMapper modelMapper, PagedResponseMapper pagedResponseMapper) {
        this.genreRepository = genreRepository;
        this.movieRepository = movieRepository;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<GenreResponseDTO> getAllGenres(Pageable pageable) {
        Page<Genre> genrePage = genreRepository.findAll(pageable);
        return pagedResponseMapper.toPagedResponse(genrePage, GenreResponseDTO.class);
    }

    @Transactional(readOnly = true)
    public GenreResponseDTO getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found: " + id));
        return modelMapper.map(genre, GenreResponseDTO.class);
    }

    @Transactional
    public GenreResponseDTO createGenre(GenreRequestDTO genreRequestDTO) {
        Genre genre = modelMapper.map(genreRequestDTO, Genre.class);
        if (genreRepository.findByNameIgnoreCase(genre.getName()).isPresent()) {
            throw new IllegalArgumentException("Genre with name '" + genre.getName() + "' already exists.");
        }
        Genre savedGenre = genreRepository.save(genre);
        return modelMapper.map(savedGenre, GenreResponseDTO.class);
    }

    @Transactional
    public GenreResponseDTO updateGenre(Long id, GenreRequestDTO genreRequestDTO) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found! id:" + id));

        genreRepository.findByNameIgnoreCase(genreRequestDTO.getName()).ifPresent(existingGenre -> {
            if (!existingGenre.getId().equals(id)) {
                throw new IllegalArgumentException("Genre with name '" + genreRequestDTO.getName() + "' already exists.");
            }
        });

        modelMapper.map(genreRequestDTO, genre);
        Genre updatedGenre = genreRepository.save(genre);
        return modelMapper.map(updatedGenre, GenreResponseDTO.class);
    }

    @Transactional
    public void deleteGenre(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genre not found! id: " + id));

        if (movieRepository.existsByGenresId(id)) {
            throw new IllegalStateException("Cannot delete genre with ID " + id + " as it is associated with existing movies.");
        }
        genreRepository.delete(genre);
    }
}