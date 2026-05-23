package library.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.dto.BorrowRecordView;
import library.dto.BorrowRequest;
import library.entity.BorrowRecord;
import library.service.BorrowRecordService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "借阅管理", description = "借阅记录查询与处理接口")
@RestController
@RequestMapping("/api/admin/borrows")
public class BorrowController {

    private final BorrowRecordService borrowRecordService;

    public BorrowController(BorrowRecordService borrowRecordService) {
        this.borrowRecordService = borrowRecordService;
    }

    @Operation(summary = "创建借阅", description = "校验用户和副本后创建借阅记录")
    @PostMapping
    public Result<Void> create(@RequestBody BorrowRequest request) {
        borrowRecordService.createBorrow(request.getUserId(), request.getCopyId());
        return Result.ok();
    }

    @Operation(summary = "分页查询借阅记录", description = "返回读者、图书、副本码等展示字段，支持按状态、用户ID、图书ID、读者/书名/ISBN/副本码关键字、月份和日期区间筛选")
    @GetMapping
    public Result<Page<BorrowRecordView>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<BorrowRecord> page = new Page<>(current, size);
        Page<BorrowRecordView> result = borrowRecordService.getBorrowViewPage(page, status, userId, bookId,
                keyword, bookTitle, month, startDate, endDate);
        return Result.ok(result);
    }

    @Operation(summary = "归还图书")
    @PutMapping("/{id}/return")
    public Result<Void> doReturn(@PathVariable Long id) {
        borrowRecordService.doReturn(id);
        return Result.ok();
    }

    @Operation(summary = "刷新逾期状态", description = "将所有超期未还的借阅记录标记为已逾期")
    @PutMapping("/refresh-status")
    public Result<Void> refreshStatus() {
        borrowRecordService.refreshOverdueStatus();
        return Result.ok();
    }
}
