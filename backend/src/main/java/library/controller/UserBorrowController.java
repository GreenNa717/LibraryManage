package library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.common.SecurityUtils;
import library.dto.BorrowActionResult;
import library.dto.BorrowRecordView;
import library.dto.ReturnActionResult;
import library.dto.UserBorrowRequest;
import library.entity.BorrowRecord;
import library.service.BookInfoService;
import library.service.BorrowRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户端借阅", description = "普通用户借阅与归还接口")
@RestController
@RequestMapping("/api/user/borrows")
public class UserBorrowController {

    private final BorrowRecordService borrowRecordService;
    private final BookInfoService bookInfoService;

    public UserBorrowController(BorrowRecordService borrowRecordService, BookInfoService bookInfoService) {
        this.borrowRecordService = borrowRecordService;
        this.bookInfoService = bookInfoService;
    }

    @Operation(summary = "创建我的借阅", description = "当前登录用户借阅图书（支持副本ID或副本码），返回借阅结果")
    @PostMapping
    public Result<BorrowActionResult> createMine(@RequestBody UserBorrowRequest request) {
        Long copyId = request.getCopyId();
        if (copyId == null && request.getCopyCode() != null && !request.getCopyCode().isBlank()) {
            Map<String, Object> found = bookInfoService.findByCopyCode(request.getCopyCode().trim());
            if (found != null && found.get("copyId") != null) {
                copyId = Long.valueOf(found.get("copyId").toString());
            }
        }
        if (copyId == null) {
            throw new RuntimeException("请提供有效的副本ID或副本码");
        }
        return Result.ok(borrowRecordService.createBorrowWithResult(SecurityUtils.currentUserId(), copyId));
    }

    @Operation(summary = "查询我的借阅记录", description = "仅返回当前用户自己的借阅记录，包含书名、作者、封面等展示字段")
    @GetMapping("/me")
    public Result<Page<BorrowRecordView>> listMine(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<BorrowRecord> page = new Page<>(current, size);
        Page<BorrowRecordView> viewPage = borrowRecordService.getBorrowViewPage(page, status, SecurityUtils.currentUserId(), null, null, null, null, null, null);
        return Result.ok(viewPage);
    }

    @Operation(summary = "归还我的图书", description = "归还借阅记录，返回归还结果")
    @PutMapping("/{id}/return")
    public Result<ReturnActionResult> returnMine(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.getById(id);
        if (record == null) {
            throw new RuntimeException("借阅记录不存在");
        }
        if (!SecurityUtils.currentUserId().equals(record.getUserId())) {
            throw new RuntimeException("只能归还自己的借阅记录");
        }
        return Result.ok(borrowRecordService.doReturnWithResult(id));
    }

    @Operation(summary = "按副本码归还我的图书", description = "移动端扫码归还：传入副本条码，系统自动查找当前用户未归还的借阅记录，返回归还结果")
    @PutMapping("/return-by-copy-code")
    public Result<ReturnActionResult> returnMineByCopyCode(@RequestBody UserBorrowRequest request) {
        if (request == null || request.getCopyCode() == null || request.getCopyCode().isBlank()) {
            throw new RuntimeException("请提供有效的副本码");
        }
        Map<String, Object> found = bookInfoService.findByCopyCode(request.getCopyCode().trim());
        if (found == null || found.get("copyId") == null) {
            throw new RuntimeException("未找到该副本");
        }
        Long copyId = Long.valueOf(found.get("copyId").toString());
        BorrowRecord record = borrowRecordService.getOne(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, SecurityUtils.currentUserId())
                .eq(BorrowRecord::getCopyId, copyId)
                .ne(BorrowRecord::getStatus, 1)
                .orderByDesc(BorrowRecord::getId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new RuntimeException("没有找到当前用户未归还的借阅记录");
        }
        return Result.ok(borrowRecordService.doReturnWithResult(record.getId()));
    }
}
