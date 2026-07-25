package com.stylemind.common.web;

import com.stylemind.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationSupportTest {

    @Test
    void customerListPageable_usesDefaultPageSizeAndStableSort() {
        Pageable pageable = PaginationSupport.customerListPageable(null, null);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void customerListPageable_allowsBoundedPageAndSize() {
        Pageable pageable = PaginationSupport.customerListPageable(2, 50);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void customerListPageable_rejectsNegativePage() {
        assertThatThrownBy(() -> PaginationSupport.customerListPageable(-1, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void customerListPageable_rejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> PaginationSupport.customerListPageable(0, 51))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void customerListPageable_acceptsWhitelistedSortAndAddsStableIdTieBreaker() {
        Pageable pageable = PaginationSupport.customerListPageable(0, 10, "createdAt,asc");

        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void customerListPageable_rejectsUnknownSortField() {
        assertThatThrownBy(() -> PaginationSupport.customerListPageable(0, 10, "status,desc"))
                .isInstanceOf(BusinessException.class);
    }
}
