package br.ifsp.my_movinhos.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ifsp.my_movinhos.dto.*;
import br.ifsp.my_movinhos.dto.page.PagedResponse;
import br.ifsp.my_movinhos.exception.InvalidReviewStateException;
import br.ifsp.my_movinhos.exception.ResourceNotFoundException;
import br.ifsp.my_movinhos.mapper.PagedResponseMapper;
import br.ifsp.my_movinhos.model.ContentFlag;
import br.ifsp.my_movinhos.model.Review;
import br.ifsp.my_movinhos.model.User;
import br.ifsp.my_movinhos.model.key.UserReviewId;
import br.ifsp.my_movinhos.repository.ContentFlagRepository;
import br.ifsp.my_movinhos.repository.ReviewRepository;
import br.ifsp.my_movinhos.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentFlagService {

    private final ContentFlagRepository contentFlagRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final ModelMapper modelMapper;
    private final PagedResponseMapper pagedResponseMapper;

    @Value("${app.reviews.flags.auto-hide-threshold:10}")
    private int autoHideThreshold;

    public ContentFlagService(ContentFlagRepository contentFlagRepository,
                              ReviewRepository reviewRepository,
                              UserRepository userRepository,
                              ReviewService reviewService,
                              ModelMapper modelMapper,
                              PagedResponseMapper pagedResponseMapper) {
        this.contentFlagRepository = contentFlagRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewService = reviewService;
        this.modelMapper = modelMapper;
        this.pagedResponseMapper = pagedResponseMapper;
    }

    @Transactional
    public ContentFlagResponseDTO flagReview(Long reviewId, Long reporterUserId, ContentFlagRequestDTO requestDTO) {
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter User not found with id: " + reporterUserId));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        UserReviewId id = new UserReviewId(reporterUserId, reviewId);
        if (contentFlagRepository.existsById(id)) {
            throw new InvalidReviewStateException("User has already flagged this review: " + reporterUserId);
        }

        if (review.getUserWatched() != null && review.getUserWatched().getUser().getId().equals(reporterUserId)) {
            throw new InvalidReviewStateException("Users cannot flag their own reviews.");
        }

        ContentFlag contentFlag = new ContentFlag(reporter, review, requestDTO.getFlagReason());
        ContentFlag savedFlag = contentFlagRepository.save(contentFlag);

        long currentFlags = review.getFlags().size();
        if (currentFlags >= autoHideThreshold && !review.isHidden()) {
            reviewService.toggleHideReview(reviewId, true);
        }

        return ContentFlagResponseDTO.builder()
                .reviewId(review.getId())
                .reporterUserId(reporter.getId())
                .reporterUsername(reporter.getUsername())
                .flagReason(savedFlag.getFlagReason())
                .createdAt(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .updatedAt(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<FlaggedReviewResponseDTO> getHeavilyFlaggedReviews(int minFlags, Pageable pageable) {
        List<Review> flaggedReviews = reviewRepository.findReviewsWithMinimumFlagsOrderByFlagsDesc(minFlags);

        if (flaggedReviews.isEmpty()) {
            Page<FlaggedReviewResponseDTO> emptyPage = Page.empty(pageable);
            return pagedResponseMapper.toPagedResponse(emptyPage, FlaggedReviewResponseDTO.class);
        }

        List<FlaggedReviewResponseDTO> flaggedReviewDTOs = flaggedReviews.stream()
                .map(review -> FlaggedReviewResponseDTO.builder()
                        .review(mapToReviewResponseDTO(review))
                        .flagCount((long) review.getFlags().size())
                        .build())
                .collect(Collectors.toList());

        Page<FlaggedReviewResponseDTO> page = new PageImpl<>(flaggedReviewDTOs, pageable, flaggedReviewDTOs.size());

        return pagedResponseMapper.toPagedResponse(page, FlaggedReviewResponseDTO.class);
    }


    private ReviewResponseDTO mapToReviewResponseDTO(Review review) {
        ReviewResponseDTO dto = modelMapper.map(review, ReviewResponseDTO.class);

        if (review.getUserWatched() != null && review.getUserWatched().getUser() != null) {
            dto.setUsername(review.getUserWatched().getUser().getUsername());
            dto.setUserId(review.getUserWatched().getUser().getId());
        }

        if (review.getUserWatched() != null) {
            dto.setMovieId(review.getUserWatched().getMovie().getId());
            dto.setMovieTitle(review.getUserWatched().getMovie().getTitle());
        }

        // createdAt e updatedAt, se não vierem pelo modelMapper, copie manualmente:
        if (review.getCreatedAt() != null) {
            dto.setCreatedAt(LocalDateTime.now());
        }
        if (review.getUpdatedAt() != null) {
            dto.setUpdatedAt(LocalDateTime.now());
        }

        return dto;
    }


}
