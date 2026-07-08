package com.stylemind.product.service;

import com.stylemind.product.dto.CategoryRequest;
import com.stylemind.product.entity.Category;
import com.stylemind.product.repository.CategoryRepository;
import com.stylemind.product.repository.ProductCategoryRepository;
import com.stylemind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(Long parentId) {
        // With an explicit parentId, return that parent's direct children (drill-down).
        // Without one (the customer Shop filter), return the full flat list so every
        // usable category is selectable — not just top-level roots. Products are tagged
        // with leaf/child categories, so a roots-only list hid most of the catalogue.
        if (parentId != null) {
            return categoryRepository.findByParentId(parentId);
        }
        return categoryRepository.findAll();
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new BusinessException("SLUG_EXISTS", "Slug danh mục đã tồn tại", 400);
        }
        if (request.getParentId() != null && !categoryRepository.existsById(request.getParentId())) {
            throw new BusinessException("PARENT_NOT_FOUND", "Danh mục cha không tồn tại", 400);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .parentId(request.getParentId())
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Không tìm thấy danh mục", 404));

        if (!category.getSlug().equals(request.getSlug()) && categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new BusinessException("SLUG_EXISTS", "Slug danh mục đã tồn tại", 400);
        }
        if (request.getParentId() != null && !categoryRepository.existsById(request.getParentId())) {
            throw new BusinessException("PARENT_NOT_FOUND", "Danh mục cha không tồn tại", 400);
        }
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BusinessException("INVALID_PARENT", "Danh mục cha không thể là chính nó", 400);
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParentId(request.getParentId());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Không tìm thấy danh mục", 404));
        
        List<Category> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("CATEGORY_HAS_CHILDREN", "Không thể xóa danh mục đang có danh mục con", 409);
        }

        if (productCategoryRepository.existsByCategoryId(id)) {
            throw new BusinessException("CATEGORY_IN_USE", "Không thể xóa danh mục đang được sản phẩm sử dụng", 409);
        }

        categoryRepository.delete(category);
    }
}
