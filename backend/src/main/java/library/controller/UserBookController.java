package library.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.Result;
import library.dto.CopyLookupView;
import library.entity.BookInfo;
import library.service.BookInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户端图书", description = "普通用户可访问的图书查询接口")
@RestController
@RequestMapping("/api/user/books")
public class UserBookController {

    private final BookInfoService bookInfoService;

    public UserBookController(BookInfoService bookInfoService) {
        this.bookInfoService = bookInfoService;
    }

    @Operation(summary = "分页查询图书列表", description = "支持按书名或作者模糊搜索，支持仅查看可借图书")
    @GetMapping
    public Result<Page<BookInfo>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean availableOnly) {
        Page<BookInfo> page = new Page<>(current, size);
        page = bookInfoService.getPage(page, keyword, categoryId, null, null, null, null, availableOnly);
        return Result.ok(page);
    }

    @Operation(summary = "根据ID查询图书")
    @GetMapping("/{id:\\d+}")
    public Result<BookInfo> getById(@PathVariable Long id) {
        return Result.ok(bookInfoService.getById(id));
    }

    @Operation(summary = "根据ISBN查询图书")
    @GetMapping("/lookup")
    public Result<BookInfo> lookupByIsbn(@RequestParam String isbn) {
        BookInfo book = bookInfoService.findByIsbn(isbn);
        if (book == null) return Result.error("未找到ISBN为 " + isbn + " 的图书");
        return Result.ok(book);
    }

    @Operation(summary = "按副本编号查询图书", description = "返回副本扫码确认页专用视图，包含是否可借/可归还等状态")
    @GetMapping("/copy-lookup")
    public Result<CopyLookupView> copyLookup(@RequestParam String code) {
        CopyLookupView view = bookInfoService.findCopyLookupView(code);
        if (view == null) return Result.error("副本不存在");
        return Result.ok(view);
    }
}
