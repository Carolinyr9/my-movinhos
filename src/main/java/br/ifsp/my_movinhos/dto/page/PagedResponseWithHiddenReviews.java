package br.ifsp.my_movinhos.dto.page;

import java.util.List;

import br.ifsp.my_movinhos.dto.ReviewResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PagedResponseWithHiddenReviews {
    private List<ReviewResponseDTO> visibleReviews;
    private List<Long> hiddenReviewIds;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
