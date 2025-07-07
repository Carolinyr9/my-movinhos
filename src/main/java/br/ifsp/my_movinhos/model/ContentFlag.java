package br.ifsp.my_movinhos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.ifsp.my_movinhos.model.key.UserReviewId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"review"})
@Table(name = "content_flags")
public class ContentFlag {

    @EmbeddedId
    private UserReviewId id;

    @Setter
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "flag_reason", nullable = false)
    private String flagReason;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reviewId")
    @JoinColumn(name = "review_id", referencedColumnName = "id")
    private Review review;

    public ContentFlag(Long userId, Review review, String flagReason) {
        this.userId = userId;
        this.review = review;
        this.id = new UserReviewId(userId, review.getId());
        this.flagReason = flagReason;
    }
}
