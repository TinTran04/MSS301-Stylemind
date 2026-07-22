package com.stylemind.product.service;

import com.stylemind.product.dto.CategoryRequest;
import com.stylemind.product.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> getAllCategories();

    List<Category> getCategories(Long parentId);

    Category createCategory(CategoryRequest request);

    Category updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
