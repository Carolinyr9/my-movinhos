package br.ifsp.my_movinhos.service;

import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import br.ifsp.my_movinhos.dto.ReviewAveragesDTO;
import br.ifsp.my_movinhos.dto.ReviewRequestDTO;
import br.ifsp.my_movinhos.dto.ReviewResponseDTO;
import br.ifsp.my_movinhos.dto.UserResponseDTO;
import br.ifsp.my_movinhos.dto.page.PagedResponseWithHiddenReviews;
import br.ifsp.my_movinhos.exception.InvalidReviewStateException;
import br.ifsp.my_movinhos.exception.ResourceNotFoundException;
import br.ifsp.my_movinhos.external.auth.AuthServiceClient;
import br.ifsp.my_movinhos.mapper.PagedResponseMapper;
import br.ifsp.my_movinhos.model.*;
import br.ifsp.my_movinhos.model.key.UserMovieId;
import br.ifsp.my_movinhos.repository.MovieRepository;
import br.ifsp.my_movinhos.repository.ReviewRepository;
import br.ifsp.my_movinhos.repository.UserWatchedRepository;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AuthServiceClient authServiceClient;
    private final MovieRepository movieRepository;
    private final UserWatchedRepository userWatchedRepository;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    public ReviewService(ReviewRepository reviewRepository,
                         AuthServiceClient authServiceClient,
                         MovieRepository movieRepository,
                         UserWatchedRepository userWatchedRepository,
                         ModelMapper modelMapper,
                         PagedResponseMapper pagedResponseMapper) {
        this.reviewRepository = reviewRepository;
        this.authServiceClient = authServiceClient;
        this.movieRepository = movieRepository;
        this.userWatchedRepository = userWatchedRepository;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
    }

    @Transactional
    public ReviewResponseDTO createReview(Long userId, Long movieId, ReviewRequestDTO reviewRequestDTO) {
        UserResponseDTO userResponseDTO = null;
        try {
            userResponseDTO = authServiceClient.getUserById(userId);
        } catch (RuntimeException e) { 
            throw new ResourceNotFoundException("Reporter User not found with id: " + userId);
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        UserMovieId userMovieId = new UserMovieId(userResponseDTO.getId(), movie.getId());
        UserWatched userWatched = userWatchedRepository.findById(userMovieId)
                .orElseThrow(() -> new InvalidReviewStateException("User has not watched this movie. Cannot create review."));

        if (userWatched.getReview() != null) {
            throw new InvalidReviewStateException("A review already exists for this watched movie by this user.");
        }

        Review review = modelMapper.map(reviewRequestDTO, Review.class);
        review.setUserWatched(userWatched);
        userWatched.setReview(review);
        userWatchedRepository.save(userWatched);

        return toDTO(userWatched.getReview());
    }

    @Transactional(readOnly = true)
    public ReviewResponseDTO getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        return toDTO(review);
    }

    @Transactional(readOnly = true)
    public PagedResponseWithHiddenReviews getReviewsByMovie(Long movieId, Pageable pageable) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie not found with id: " + movieId);
        }

        Page<Review> reviewPage = reviewRepository.findByUserWatched_Movie_Id(movieId, pageable);

        List<Long> hiddenReviewIds = reviewPage.stream()
            .filter(Review::isHidden)
            .map(Review::getId)
            .toList();

        List<ReviewResponseDTO> visibleReviewsDTO = reviewPage.stream()
            .filter(review -> !review.isHidden())
            .map(this::toDTO)
            .toList();

        return new PagedResponseWithHiddenReviews(
            visibleReviewsDTO,
            hiddenReviewIds,
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.isLast()
        );
    }


    @Transactional(readOnly = true)
    public PagedResponseWithHiddenReviews getReviewsByUser(Long userId, Pageable pageable) {
        if (authServiceClient.getUserById(userId) == null) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Page<Review> reviewPage = reviewRepository.findByUserWatched_Id_UserId(userId, pageable);

        List<Long> hiddenReviewIds = reviewPage.stream()
            .filter(Review::isHidden)
            .map(Review::getId)
            .toList();

        List<ReviewResponseDTO> visibleReviewsDTO = reviewPage.stream()
            .filter(review -> !review.isHidden())
            .map(this::toDTO)
            .toList();

        return new PagedResponseWithHiddenReviews(
            visibleReviewsDTO,
            hiddenReviewIds,
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.isLast()
        );
    }

    @Transactional
    public ReviewResponseDTO updateReview(Long reviewId, Long userId, ReviewRequestDTO reviewRequestDTO) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUserWatched().getUserId().equals(userId)) {
            throw new AccessDeniedException("User is not authorized to update this review.");
        }

        LocalDateTime date = review.getCreatedAt() != null
                ? LocalDateTime.ofInstant(review.getCreatedAt(), java.time.ZoneId.systemDefault())
                : null;

        modelMapper.map(reviewRequestDTO, review);
        Review updatedReview = reviewRepository.save(review);
        ReviewResponseDTO responseDTO = toDTO(updatedReview);
        responseDTO.setCreatedAt(date); 
        return responseDTO;
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userIdPrincipal) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        UserWatched userWatched = review.getUserWatched();
        if (userWatched != null) {
            userWatched.setReview(null);
            userWatchedRepository.save(userWatched);
        }
        reviewRepository.delete(review);
    }

    @Transactional
    public ReviewResponseDTO likeReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        review.setLikesCount(review.getLikesCount() + 1);
        return toDTO(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponseDTO toggleHideReview(Long reviewId, boolean hide) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        review.setHidden(hide);
        return toDTO(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public void exportAsPdf(HttpServletResponse response) throws Exception {
        List<Review> reviews = reviewRepository.findAll();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reviews.pdf");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);

        document.open();
        document.add(new Paragraph("Lista de Avaliações"));

        UserResponseDTO userResponseDTO = null;
        for (Review review : reviews) {
            
            try {
                userResponseDTO = authServiceClient.getUserById(review.getUserWatched().getUserId());
            } catch (RuntimeException e) { 
                throw new ResourceNotFoundException("Reporter User not found with id: " + review.getUserWatched().getUserId());
            }

            document.add(new Paragraph("Review ID: " + review.getId()));
            document.add(new Paragraph("Usuário: " + userResponseDTO.getUsername()));
            document.add(new Paragraph("Filme: " + review.getUserWatched().getMovie().getTitle()));
            document.add(new Paragraph("Nota Geral: " + review.getGeneralScore()));
            document.add(new Paragraph("Nota de Direção: " + review.getDirectionScore()));
            document.add(new Paragraph("Nota de Roteiro: " + review.getScreenplayScore()));
            document.add(new Paragraph("Nota de Cinematografia: " + review.getCinematographyScore()));
            document.add(new Paragraph("Conteúdo: " + review.getContent()));
            document.add(new Paragraph("------"));
        }

        document.close();
        OutputStream os = response.getOutputStream();
        baos.writeTo(os);
        os.flush();
    }

    public String getUserStatistics(Pageable pageable, Long userId) {
        UserResponseDTO userResponseDTO = null;
        try {
            userResponseDTO = authServiceClient.getUserById(userId);
        } catch (RuntimeException e) { 
            throw new ResourceNotFoundException("Reporter User not found with id: " + userId);
        }

        long totalReviews = reviewRepository.countByUserWatched_Id_UserId(userId);
        long totalLikes = reviewRepository.sumLikesCountByUserWatchedIdUserId(userId);
        double averageGeneralScore = reviewRepository.calculateAverageGeneralScoreByUserWatchedIdUserId(userId);

        return String.format("O usuário %s fez %d reviews, recebeu %d likes e tem uma média geral nas avaliações de %.2f.",
                userResponseDTO.getUsername(), totalReviews, totalLikes, averageGeneralScore);
    }

    public ReviewAveragesDTO getAverageWeighted(Pageable pageable, Long userId) {
        UserResponseDTO userResponseDTO = null;
        try {
            userResponseDTO = authServiceClient.getUserById(userId);
        } catch (RuntimeException e) { 
            throw new ResourceNotFoundException("Reporter User not found with id: " + userId);
        }

        double directionAvg = reviewRepository.calculateAverageDirectionScoreByUserWatchedIdUserId(userId);
        double screenplayAvg = reviewRepository.calculateAverageScreenplayScoreByUserWatchedIdUserId(userId);
        double cinematographyAvg = reviewRepository.calculateAverageCinematographyScoreByUserWatchedIdUserId(userId);
        double generalAvg = reviewRepository.calculateAverageGeneralScoreByUserWatchedIdUserId(userId);

        return new ReviewAveragesDTO(directionAvg, screenplayAvg, cinematographyAvg, generalAvg);
    }


    private ReviewResponseDTO toDTO(Review review) {
        UserResponseDTO userResponseDTO = null;
        try {
            userResponseDTO = authServiceClient.getUserById(review.getUserWatched().getUserId());
        } catch (RuntimeException e) { 
            throw new ResourceNotFoundException("Reporter User not found with id: " + review.getUserWatched().getUserId());
        }
        
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .content(review.getContent())
                .directionScore(review.getDirectionScore())
                .screenplayScore(review.getScreenplayScore())
                .cinematographyScore(review.getCinematographyScore())
                .generalScore(review.getGeneralScore())
                .likesCount(review.getLikesCount())
                .hidden(review.isHidden())
                .createdAt(review.getCreatedAt() != null ?
                        LocalDateTime.ofInstant(review.getCreatedAt(), java.time.ZoneId.systemDefault()) : null)
                .updatedAt(review.getUpdatedAt() != null ?
                        LocalDateTime.ofInstant(review.getUpdatedAt(), java.time.ZoneId.systemDefault()) : null)
                .userId(userResponseDTO.getId())
                .username(userResponseDTO.getUsername())
                .movieId(review.getUserWatched().getMovie().getId())
                .movieTitle(review.getUserWatched().getMovie().getTitle())
                .build();
    }


}
