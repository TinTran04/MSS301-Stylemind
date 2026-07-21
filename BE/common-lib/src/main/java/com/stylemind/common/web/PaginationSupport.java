package com.stylemind.common.web;

import com.stylemind.common.constant.ErrorCode;
import com.stylemind.common.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationSupport {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private PaginationSupport() {
    }

    public static Pageable customerListPageable(Integer page, Integer size) {
        return customerListPageable(page, size, null);
    }

    public static Pageable customerListPageable(Integer page, Integer size, String sort) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;
        return customerPageable(resolvedPage, resolvedSize, sort, Set.of("createdAt", "id"));
    }

    public static Pageable customerPageable(int page, int size, String sort, Set<String> allowedSortFields) {
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return PageRequest.of(page, size, parseSort(sort, allowedSortFields));
    }

    private static Sort parseSort(String sort, Set<String> allowedSortFields) {
        if (sort == null || sort.isBlank()) {
            return defaultSort();
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!allowedSortFields.contains(property)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.ASC;
        } else if (parts.length > 1 && !"desc".equalsIgnoreCase(parts[1].trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Sort requested = Sort.by(direction, property);
        if (!"id".equals(property)) {
            requested = requested.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return requested;
    }

    private static Sort defaultSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
