package com.stylemind.product.controller;

import com.stylemind.common.dto.ApiResponse;
import com.stylemind.product.dto.CategoryResponse;
import com.stylemind.product.entity.Category;
import com.stylemind.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(required = false) Long parentId) {
        List<Category> categories = parentId == null
                ? categoryRepository.findAll()
                : categoryRepository.findByParentId(parentId);

        List<CategoryResponse> response = categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục sản phẩm thành công", response));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .slug(category.getSlug())
                .build();
    }
}
