package library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据看板", description = "统计数据接口")
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final BookInfoService bookInfoService;
    private final BorrowRecordService borrowRecordService;
    private final OperationLogService operationLogService;
    private final ShelfLocationService shelfLocationService;

    public DashboardController(BookInfoService bookInfoService,
                               BorrowRecordService borrowRecordService,
                               OperationLogService operationLogService,
                               ShelfLocationService shelfLocationService) {
        this.bookInfoService = bookInfoService;
        this.borrowRecordService = borrowRecordService;
        this.operationLogService = operationLogService;
        this.shelfLocationService = shelfLocationService;
    }

    @Operation(summary = "获取统计数据", description = "馆藏总数、当前借阅、逾期数量等")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = Map.of(
                "totalBooks", bookInfoService.getTotalBooks(),
                "totalCopies", bookInfoService.getTotalCopies(),
                "borrowingCount", borrowRecordService.getBorrowingCount(),
                "overdueCount", borrowRecordService.getOverdueCount(),
                "todayBorrowCount", borrowRecordService.getTodayBorrowCount(),
                "readerCount", bookInfoService.getReaderCount()
        );
        return Result.ok(stats);
    }

    @Operation(summary = "热门图书Top N", description = "按借阅次数降序排列")
    @GetMapping("/hot-books")
    public Result<List<Map<String, Object>>> hotBooks(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(bookInfoService.getHotBooks(limit));
    }

    @Operation(summary = "每月借阅趋势", description = "近12个月的借阅量统计")
    @GetMapping("/monthly-borrows")
    public Result<List<Map<String, Object>>> monthlyBorrows() {
        return Result.ok(borrowRecordService.getMonthlyBorrows());
    }

    @Operation(summary = "图书库存概览", description = "每本书的总库存与可借库存")
    @GetMapping("/book-stocks")
    public Result<List<Map<String, Object>>> bookStocks() {
        return Result.ok(bookInfoService.getBookStocks());
    }

    @Operation(summary = "最近操作日志", description = "馆务操作流水线数据")
    @GetMapping("/activity-feed")
    public Result<List<Map<String, Object>>> activityFeed(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(operationLogService.getRecentLogs(limit));
    }

    @Operation(summary = "资产健康度", description = "可借/借出/丢失的数量分布")
    @GetMapping("/inventory-health")
    public Result<Map<String, Object>> inventoryHealth() {
        return Result.ok(bookInfoService.getInventoryHealth());
    }

    @Operation(summary = "库位饱和度", description = "各库位空间占用率")
    @GetMapping("/shelf-capacity")
    public Result<List<Map<String, Object>>> shelfCapacity() {
        return Result.ok(shelfLocationService.getCapacityData());
    }

    @Operation(summary = "异常待办", description = "逾期/丢失/低库存预警")
    @GetMapping("/exceptions")
    public Result<Map<String, Object>> exceptions() {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("overdue", borrowRecordService.getOverdueDetails());
        result.put("lost", bookInfoService.getInventoryHealth().get("lost"));
        result.put("lowStock", bookInfoService.getLowStockBooks(1));
        return Result.ok(result);
    }
}
