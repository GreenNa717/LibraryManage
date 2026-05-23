package library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.entity.BookCategory;
import library.service.BookCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类管理", description = "图书分类CRUD接口")
@RestController
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final BookCategoryService categoryService;

    public CategoryController(BookCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "查询分类列表", description = "按排序值升序返回所有分类")
    @GetMapping
    public Result<List<BookCategory>> list() {
        List<BookCategory> list = categoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BookCategory>()
                        .orderByAsc(BookCategory::getSort)
        );
        return Result.ok(list);
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> add(@RequestBody BookCategory category) {
        categoryService.save(category);
        return Result.ok();
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BookCategory category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.ok();
    }
}