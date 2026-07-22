package com.stylemind.product.service;

import com.stylemind.common.exception.BusinessException;
import com.stylemind.product.entity.Category;
import com.stylemind.product.repository.CategoryRepository;
import com.stylemind.product.repository.ProductCategoryRepository;
import com.stylemind.product.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategories_returnsRootAndChildCategories() {
        List<Category> categories = List.of(
                Category.builder().id(1L).name("Áo").slug("ao").build(),
                Category.builder().id(2L).name("Áo sơ mi").slug("ao-so-mi").parentId(1L).build());
        when(categoryRepository.findAll()).thenReturn(categories);

        List<Category> result = categoryService.getAllCategories();

        assertEquals(categories, result);
    }

    @Test
    void getCategories_noParent_returnsFullFlatList() {
        // Public Shop filter: must include child categories, not just roots.
        List<Category> all = List.of(
                Category.builder().id(1L).name("Áo").slug("ao").build(),
                Category.builder().id(2L).name("Quần").slug("quan").build(),
                Category.builder().id(3L).name("Áo sơ mi").slug("ao-so-mi").parentId(1L).build(),
                Category.builder().id(4L).name("Áo polo").slug("ao-polo").parentId(1L).build());
        when(categoryRepository.findAll()).thenReturn(all);
        when(productCategoryRepository.existsByCategoryId(anyLong())).thenReturn(true);

        List<Category> result = categoryService.getCategories(null);

        assertEquals(all, result);
    }

    @Test
    void getCategories_withParent_returnsChildren() {
        List<Category> children = List.of(
                Category.builder().id(3L).name("Áo sơ mi").slug("ao-so-mi").parentId(1L).build());
        when(categoryRepository.findByParentId(1L)).thenReturn(children);
        when(productCategoryRepository.existsByCategoryId(anyLong())).thenReturn(true);

        List<Category> result = categoryService.getCategories(1L);

        assertEquals(children, result);
    }

    @Test
    void deleteCategory_withChildCategory_returnsConflict() {
        Category category = Category.builder().id(1L).name("Áo").slug("ao").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(
                Category.builder().id(2L).name("Áo sơ mi").slug("ao-so-mi").parentId(1L).build()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.deleteCategory(1L));

        assertEquals("CATEGORY_HAS_CHILDREN", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());
    }

    @Test
    void deleteCategory_withAssignedProducts_returnsConflict() {
        Category category = Category.builder().id(1L).name("Áo").slug("ao").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of());
        when(productCategoryRepository.existsByCategoryId(1L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.deleteCategory(1L));

        assertEquals("CATEGORY_IN_USE", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());
    }
}
