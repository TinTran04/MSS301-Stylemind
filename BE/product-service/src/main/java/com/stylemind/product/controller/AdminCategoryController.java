package com.stylemind.product.controller;

import com.stylemind.product.dto.CategoryRequest;
import com.stylemind.product.entity.Category;
import com.stylemind.product.service.CategoryService;
import com.stylemind.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }
}
