package com.stylemind.product.service;

import com.stylemind.product.dto.CategoryRequest;
import com.stylemind.product.entity.Category;
import com.stylemind.product.repository.CategoryRepository;
import com.stylemind.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Category> getCategories(Long parentId) {
        if (parentId != null) {
            return categoryRepository.findByParentId(parentId);
        }
        return categoryRepository.findByParentIdIsNull();
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
            throw new BusinessException("CATEGORY_HAS_CHILDREN", "Không thể xóa danh mục đang có danh mục con", 400);
        }

        // Note: productRepository.countByCategoryId(id) should be used if we had it, but we can just use findByCategoryIdAndStatus although it might be slow.
        // Or we can just use categoryRepository delete, if DB constraints fail it throws DataIntegrityViolationException.
        // A better check would be added to ProductRepository: boolean existsByCategoryId(Long categoryId);
        // We will add it to ProductRepository next.
        
        categoryRepository.delete(category);
    }
}
