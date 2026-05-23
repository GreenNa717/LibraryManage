package library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import library.common.BarcodeUtil;
import library.common.Result;
import library.dto.AddBookRequest;
import library.dto.CopyBatchRequest;
import library.dto.CopyLocationRequest;
import library.dto.CopyStatusRequest;
import library.dto.ScanInboundRequest;
import library.entity.BookCopy;
import library.entity.BookInfo;
import library.entity.ShelfLocation;
import library.service.BookCopyService;
import library.service.BookInfoService;
import library.service.IsbnMetadataService;
import library.service.ShelfLocationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "图书管理", description = "图书信息CRUD接口")
@RestController
@RequestMapping("/api/admin/books")
public class BookController {

    private final BookInfoService bookInfoService;
    private final BookCopyService bookCopyService;
    private final IsbnMetadataService isbnMetadataService;
    private final ShelfLocationService shelfLocationService;

    public BookController(BookInfoService bookInfoService, BookCopyService bookCopyService, IsbnMetadataService isbnMetadataService,
                          ShelfLocationService shelfLocationService) {
        this.bookInfoService = bookInfoService;
        this.bookCopyService = bookCopyService;
        this.isbnMetadataService = isbnMetadataService;
        this.shelfLocationService = shelfLocationService;
    }

    @Operation(summary = "分页查询图书列表", description = "支持按书名/作者关键字、精确作者、ISBN、分类、库位、丢失状态筛选；ISBN兼容纯数字和横线格式")
    @GetMapping
    public Result<Page<BookInfo>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Boolean lostOnly,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn) {
        Page<BookInfo> page = new Page<>(current, size);
        page = bookInfoService.getPage(page, keyword, categoryId, locationId, lostOnly, author, isbn, null);
        return Result.ok(page);
    }

    @Operation(summary = "根据ID查询图书")
    @GetMapping("/{id:\\d+}")
    public Result<BookInfo> getById(@PathVariable Long id) {
        return Result.ok(bookInfoService.getById(id));
    }

    @Operation(summary = "根据ISBN查询图书", description = "扫码后通过ISBN查找图书信息，兼容纯数字和横线格式")
    @GetMapping("/lookup")
    public Result<BookInfo> lookupByIsbn(@RequestParam String isbn) {
        BookInfo book = bookInfoService.findByIsbn(isbn);
        if (book == null) return Result.error("未找到ISBN为 " + isbn + " 的图书");
        return Result.ok(book);
    }

    @Operation(summary = "新增图书", description = "入库并自动生成副本记录")
    @PostMapping
    public Result<Void> add(@RequestBody AddBookRequest req) {
        assertUniqueIsbn(req.getIsbn(), null);
        int qty = req.getQuantity() != null && req.getQuantity() > 0 ? req.getQuantity() : 1;
        BookInfo book = new BookInfo();
        book.setIsbn(req.getIsbn() == null ? null : req.getIsbn().trim());
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setCategoryId(req.getCategoryId());
        book.setLocationId(req.getLocationId());
        book.setCoverImage(req.getCoverImage());
        book.setDescription(req.getDescription());
        book.setTotalStock(qty);
        book.setCurrentStock(qty);
        bookInfoService.save(book);
        bookCopyService.generateCopies(book.getId(), book.getIsbn(), qty, book.getLocationId());
        adjustShelfCount(book.getLocationId(), qty);
        isbnMetadataService.saveLocalMetadata(book.getIsbn(), book.getTitle(), book.getAuthor(), book.getCoverImage());
        return Result.ok();
    }

    @Operation(summary = "更新图书信息")
    @PutMapping("/{id:\\d+}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BookInfo book) {
        BookInfo before = bookInfoService.getById(id);
        if (before == null) throw new RuntimeException("图书不存在");
        assertUniqueIsbn(book.getIsbn(), id);
        book.setId(id);
        book.setIsbn(book.getIsbn() == null ? null : book.getIsbn().trim());
        bookInfoService.updateById(book);
        if (!Objects.equals(book.getLocationId(), before.getLocationId())) {
            int stock = before.getTotalStock() == null ? 0 : before.getTotalStock();
            adjustShelfCount(before.getLocationId(), -stock);
            adjustShelfCount(book.getLocationId(), stock);
        }
        return Result.ok();
    }

    @Operation(summary = "删除图书")
    @DeleteMapping("/{id:\\d+}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        List<BookCopy> copies = bookCopyService.list(new LambdaQueryWrapper<BookCopy>()
                .eq(BookCopy::getBookInfoId, id));
        copies.forEach(copy -> adjustShelfCount(copy.getLocationId(), -1));
        bookInfoService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "获取图书的所有副本列表")
    @GetMapping("/{id:\\d+}/copies")
    public Result<List<Map<String, Object>>> getCopies(@PathVariable Long id) {
        return Result.ok(bookCopyService.getCopiesByBookId(id));
    }

    @Operation(summary = "新增图书副本", description = "为指定图书补录实体副本，并同步图书库存与库位数量")
    @PostMapping("/{id:\\d+}/copies")
    @Transactional(rollbackFor = Exception.class)
    public Result<List<Map<String, Object>>> addCopies(@PathVariable Long id, @RequestBody CopyBatchRequest request) {
        BookInfo book = bookInfoService.getById(id);
        if (book == null) throw new RuntimeException("图书不存在");
        int quantity = request != null && request.getQuantity() != null && request.getQuantity() > 0
                ? request.getQuantity()
                : 1;
        Long locationId = request != null && request.getLocationId() != null
                ? request.getLocationId()
                : book.getLocationId();
        bookCopyService.generateCopies(book.getId(), book.getIsbn(), quantity, locationId);
        book.setTotalStock((book.getTotalStock() == null ? 0 : book.getTotalStock()) + quantity);
        book.setCurrentStock((book.getCurrentStock() == null ? 0 : book.getCurrentStock()) + quantity);
        bookInfoService.updateById(book);
        adjustShelfCount(locationId, quantity);
        return Result.ok(bookCopyService.getCopiesByBookId(id));
    }

    @Operation(summary = "更新副本状态", description = "支持在馆可借与丢失之间切换，并同步图书可借库存")
    @PutMapping("/copies/{copyId}/status")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateCopyStatus(@PathVariable Long copyId, @RequestBody CopyStatusRequest request) {
        if (request == null || (request.getStatus() != 0 && request.getStatus() != 2)) {
            throw new RuntimeException("副本状态仅支持设置为在馆或丢失");
        }
        BookCopy copy = bookCopyService.getById(copyId);
        if (copy == null) throw new RuntimeException("副本不存在");
        if (copy.getStatus() != null && copy.getStatus() == 1) {
            throw new RuntimeException("借出中的副本不能直接改状态，请先完成归还");
        }
        Integer beforeStatus = copy.getStatus();
        Integer nextStatus = request.getStatus();
        if (Objects.equals(beforeStatus, nextStatus)) return Result.ok();

        copy.setStatus(nextStatus);
        bookCopyService.updateById(copy);

        BookInfo book = bookInfoService.getById(copy.getBookInfoId());
        if (book != null) {
            int currentStock = book.getCurrentStock() == null ? 0 : book.getCurrentStock();
            if (beforeStatus != null && beforeStatus == 0 && nextStatus == 2) {
                book.setCurrentStock(Math.max(0, currentStock - 1));
            } else if (beforeStatus != null && beforeStatus == 2 && nextStatus == 0) {
                book.setCurrentStock(currentStock + 1);
            }
            bookInfoService.updateById(book);
        }
        return Result.ok();
    }

    @Operation(summary = "移动副本库位", description = "调整单册副本所在库位，并同步库位当前藏书数量")
    @PutMapping("/copies/{copyId}/location")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateCopyLocation(@PathVariable Long copyId, @RequestBody CopyLocationRequest request) {
        BookCopy copy = bookCopyService.getById(copyId);
        if (copy == null) throw new RuntimeException("副本不存在");
        Long nextLocationId = request == null ? null : request.getLocationId();
        if (Objects.equals(copy.getLocationId(), nextLocationId)) return Result.ok();
        Long oldLocationId = copy.getLocationId();
        copy.setLocationId(nextLocationId);
        bookCopyService.updateById(copy);
        adjustShelfCount(oldLocationId, -1);
        adjustShelfCount(nextLocationId, 1);
        return Result.ok();
    }

    @Operation(summary = "删除图书副本", description = "删除错误录入的未借出副本，并同步图书库存与库位数量")
    @DeleteMapping("/copies/{copyId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteCopy(@PathVariable Long copyId) {
        BookCopy copy = bookCopyService.getById(copyId);
        if (copy == null) throw new RuntimeException("副本不存在");
        if (copy.getStatus() != null && copy.getStatus() == 1) {
            throw new RuntimeException("借出中的副本不能删除");
        }
        BookInfo book = bookInfoService.getById(copy.getBookInfoId());
        if (book != null) {
            book.setTotalStock(Math.max(0, (book.getTotalStock() == null ? 0 : book.getTotalStock()) - 1));
            if (copy.getStatus() != null && copy.getStatus() == 0) {
                book.setCurrentStock(Math.max(0, (book.getCurrentStock() == null ? 0 : book.getCurrentStock()) - 1));
            }
            bookInfoService.updateById(book);
        }
        adjustShelfCount(copy.getLocationId(), -1);
        bookCopyService.removeById(copyId);
        return Result.ok();
    }

    @Operation(summary = "生成单个副本条码", description = "根据副本编号生成Code 128格式条码图片")
    @GetMapping("/barcode")
    public ResponseEntity<byte[]> generateBarcode(@RequestParam String code) throws IOException {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] barcode = BarcodeUtil.generateCode128(code, 300, 100);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"barcode-" + code + ".png\"")
                .body(barcode);
    }

    @Operation(summary = "按副本编号查询", description = "扫描副本条码后查找副本及图书信息")
    @GetMapping("/copy-lookup")
    public Result<Map<String, Object>> copyLookup(@RequestParam String code) {
        return Result.ok(bookInfoService.findByCopyCode(code));
    }

    @Operation(summary = "扫码入库", description = "扫码枪输入一维码（ISBN）后自动入库，已存在则增加库存并新增副本")
    @PostMapping("/scan-inbound")
    public Result<Map<String, Object>> scanInbound(@RequestBody ScanInboundRequest request) {
        String isbn = request.getIsbn();
        if (isbn == null || isbn.isBlank()) {
            isbn = request.getScannedCode();
        }
        if (isbn == null || isbn.isBlank()) {
            return Result.error("请提供有效的一维码内容");
        }
        Map<String, Object> result = bookInfoService.scanInbound(
                isbn.trim(),
                request.getTitle(),
                request.getAuthor(),
                request.getCategoryId(),
                request.getLocationId(),
                request.getCoverImage(),
                request.getDescription(),
                request.getQuantity()
        );
        return Result.ok(result);
    }

    @Operation(summary = "按ISBN获取书籍信息", description = "通过真实ISBN从公共图书数据源获取书名、作者、封面等信息")
    @GetMapping("/isbn-metadata")
    public Result<Map<String, Object>> isbnMetadata(@RequestParam String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Result.error("ISBN不能为空");
        }
        try {
            Map<String, Object> metadata = isbnMetadataService.fetchByIsbn(isbn);
            if (metadata == null || metadata.isEmpty()) {
                return Result.error(404, "未查询到对应图书信息");
            }
            return Result.ok(metadata);
        } catch (IllegalStateException e) {
            return Result.error(502, "外部数据源不可用: " + e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "ISBN查询失败: " + e.getMessage());
        }
    }

    private void adjustShelfCount(Long locationId, int delta) {
        if (locationId == null || delta == 0) return;
        ShelfLocation location = shelfLocationService.getById(locationId);
        if (location == null) return;
        int current = location.getCurrentCount() == null ? 0 : location.getCurrentCount();
        location.setCurrentCount(Math.max(0, current + delta));
        shelfLocationService.updateById(location);
    }

    private void assertUniqueIsbn(String isbn, Long currentBookId) {
        String normalized = normalizeIsbn(isbn);
        if (normalized.isBlank()) return;
        long count = bookInfoService.count(new LambdaQueryWrapper<BookInfo>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalized)
                .ne(currentBookId != null, BookInfo::getId, currentBookId));
        if (count > 0) throw new RuntimeException("ISBN已存在，请勿重复录入同一本书");
    }

    private String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replace("-", "").replace(" ", "").trim();
    }
}
