package library.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.entity.IsbnMetadata;
import library.service.IsbnMetadataService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ISBN元数据管理", description = "本地ISBN资料库维护接口")
@RestController
@RequestMapping("/api/admin/isbn-metadata")
public class IsbnMetadataController {

    private final IsbnMetadataService isbnMetadataService;

    public IsbnMetadataController(IsbnMetadataService isbnMetadataService) {
        this.isbnMetadataService = isbnMetadataService;
    }

    @Operation(summary = "分页查询ISBN元数据", description = "支持按ISBN、书名、作者、出版社关键字和数据来源筛选；ISBN兼容纯数字和横线格式")
    @GetMapping
    public Result<Page<IsbnMetadata>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source) {
        Page<IsbnMetadata> page = new Page<>(current, size);
        return Result.ok(isbnMetadataService.getPage(page, keyword, source));
    }

    @Operation(summary = "根据ID查询ISBN元数据")
    @GetMapping("/{id}")
    public Result<IsbnMetadata> getById(@PathVariable Long id) {
        IsbnMetadata metadata = isbnMetadataService.getById(id);
        if (metadata == null) return Result.error(404, "ISBN元数据不存在");
        return Result.ok(metadata);
    }

    @Operation(summary = "新增ISBN元数据", description = "用于人工维护本地ISBN资料库，ISBN会自动去除横线后保存")
    @PostMapping
    public Result<Void> create(@RequestBody IsbnMetadata metadata) {
        isbnMetadataService.createMetadata(metadata);
        return Result.ok();
    }

    @Operation(summary = "更新ISBN元数据", description = "用于修正本地ISBN资料库中的书名、作者、出版社、封面等字段")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody IsbnMetadata metadata) {
        isbnMetadataService.updateMetadata(id, metadata);
        return Result.ok();
    }

    @Operation(summary = "删除ISBN元数据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        isbnMetadataService.deleteMetadata(id);
        return Result.ok();
    }
}
