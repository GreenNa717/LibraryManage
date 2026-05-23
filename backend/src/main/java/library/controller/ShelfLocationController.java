package library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.entity.ShelfLocation;
import library.service.ShelfLocationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "库位管理", description = "书架/库位 CRUD 接口")
@RestController
@RequestMapping("/api/admin/shelves")
public class ShelfLocationController {

    private final ShelfLocationService shelfLocationService;

    public ShelfLocationController(ShelfLocationService shelfLocationService) {
        this.shelfLocationService = shelfLocationService;
    }

    @Operation(summary = "查询库位列表")
    @GetMapping
    public Result<List<ShelfLocation>> list() {
        List<ShelfLocation> list = shelfLocationService.list(
                new LambdaQueryWrapper<ShelfLocation>().orderByAsc(ShelfLocation::getId)
        );
        return Result.ok(list);
    }

    @Operation(summary = "新增库位")
    @PostMapping
    public Result<Void> add(@RequestBody ShelfLocation location) {
        validate(location);
        if (location.getCurrentCount() == null) {
            location.setCurrentCount(0);
        }
        shelfLocationService.save(location);
        return Result.ok();
    }

    @Operation(summary = "更新库位")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ShelfLocation location) {
        validate(location);
        location.setId(id);
        shelfLocationService.updateById(location);
        return Result.ok();
    }

    @Operation(summary = "删除库位")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shelfLocationService.removeById(id);
        return Result.ok();
    }

    private void validate(ShelfLocation location) {
        if (location == null) throw new RuntimeException("请求参数不能为空");
        if (location.getLocationName() == null || location.getLocationName().trim().isEmpty()) {
            throw new RuntimeException("库位名称不能为空");
        }
        location.setLocationName(location.getLocationName().trim());
        if (location.getMaxCapacity() == null || location.getMaxCapacity() <= 0) {
            throw new RuntimeException("最大容量必须大于0");
        }
        if (location.getCurrentCount() != null && location.getCurrentCount() < 0) {
            throw new RuntimeException("当前藏书数量不能小于0");
        }
    }
}
