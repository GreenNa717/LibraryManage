package library.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import library.entity.BookInfo;
import library.entity.IsbnMetadata;
import library.mapper.BookInfoMapper;
import library.mapper.IsbnMetadataMapper;
import library.service.IsbnMetadataService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class IsbnMetadataServiceImpl implements IsbnMetadataService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BookInfoMapper bookInfoMapper;
    private final IsbnMetadataMapper isbnMetadataMapper;
    private final String googleApiKey;

    public IsbnMetadataServiceImpl(
            BookInfoMapper bookInfoMapper,
            IsbnMetadataMapper isbnMetadataMapper,
            @Value("${library.metadata.proxy.enabled:false}") boolean proxyEnabled,
            @Value("${library.metadata.proxy.host:}") String proxyHost,
            @Value("${library.metadata.proxy.port:0}") int proxyPort,
            @Value("${library.metadata.google.api-key:}") String googleApiKey) {
        this.bookInfoMapper = bookInfoMapper;
        this.isbnMetadataMapper = isbnMetadataMapper;
        this.googleApiKey = googleApiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        if (proxyEnabled && proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public Page<IsbnMetadata> getPage(Page<IsbnMetadata> page, String keyword, String source) {
        LambdaQueryWrapper<IsbnMetadata> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            String normalizedKw = normalizeIsbn(kw);
            wrapper.and(w -> w.like(IsbnMetadata::getIsbn, kw)
                    .or()
                    .apply(!normalizedKw.isBlank(), "REPLACE(REPLACE(isbn, '-', ''), ' ', '') LIKE CONCAT('%', {0}, '%')", normalizedKw)
                    .or()
                    .like(IsbnMetadata::getTitle, kw)
                    .or()
                    .like(IsbnMetadata::getAuthor, kw)
                    .or()
                    .like(IsbnMetadata::getPublisher, kw));
        }
        if (source != null && !source.isBlank()) {
            wrapper.eq(IsbnMetadata::getSource, source.trim());
        }
        wrapper.orderByDesc(IsbnMetadata::getUpdateTime).orderByDesc(IsbnMetadata::getId);
        return isbnMetadataMapper.selectPage(page, wrapper);
    }

    @Override
    public IsbnMetadata getById(Long id) {
        return isbnMetadataMapper.selectById(id);
    }

    @Override
    public void createMetadata(IsbnMetadata metadata) {
        normalizeAndValidate(metadata, null);
        isbnMetadataMapper.insert(metadata);
    }

    @Override
    public void updateMetadata(Long id, IsbnMetadata metadata) {
        IsbnMetadata existing = isbnMetadataMapper.selectById(id);
        if (existing == null) throw new RuntimeException("ISBN元数据不存在");
        metadata.setId(id);
        normalizeAndValidate(metadata, id);
        isbnMetadataMapper.updateById(metadata);
    }

    @Override
    public void deleteMetadata(Long id) {
        IsbnMetadata existing = isbnMetadataMapper.selectById(id);
        if (existing == null) throw new RuntimeException("ISBN元数据不存在");
        isbnMetadataMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> fetchByIsbn(String isbn) {
        String normalized = normalizeIsbn(isbn);
        if (normalized.isBlank()) return null;

        Map<String, Object> localBook = fetchFromBookInfo(normalized);
        if (localBook != null && !localBook.isEmpty()) {
            return localBook;
        }
        Map<String, Object> localMetadata = fetchFromLocalMetadata(normalized);
        if (localMetadata != null && !localMetadata.isEmpty()) {
            return localMetadata;
        }

        StringBuilder errors = new StringBuilder();
        Map<String, Object> openLibrary = fetchFromOpenLibrary(normalized, errors);
        if (openLibrary != null && !openLibrary.isEmpty()) {
            cacheExternalResult(openLibrary);
            return openLibrary;
        }
        Map<String, Object> openLibrarySearch = fetchFromOpenLibrarySearch(normalized, errors);
        if (openLibrarySearch != null && !openLibrarySearch.isEmpty()) {
            cacheExternalResult(openLibrarySearch);
            return openLibrarySearch;
        }
        if (googleApiKey == null || googleApiKey.isBlank()) {
            appendError(errors, "未配置 Google Books API Key，已跳过 GoogleBooks");
            throw new IllegalStateException(errors.toString());
        }
        Map<String, Object> google = fetchFromGoogleBooks(normalized, errors);
        if (google != null && !google.isEmpty()) {
            cacheExternalResult(google);
            return google;
        }
        throw new IllegalStateException(errors.isEmpty()
                ? "未命中外部书籍数据源"
                : errors.toString());
    }

    @Override
    public void saveLocalMetadata(String isbn, String title, String author, String coverImage) {
        String normalized = normalizeIsbn(isbn);
        if (normalized.isBlank() || title == null || title.isBlank()) {
            return;
        }

        IsbnMetadata metadata = isbnMetadataMapper.selectOne(new LambdaQueryWrapper<IsbnMetadata>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalized)
                .last("LIMIT 1"));
        if (metadata == null) {
            metadata = new IsbnMetadata();
            metadata.setIsbn(normalized);
            metadata.setTitle(title);
            metadata.setAuthor(author);
            metadata.setCoverImage(coverImage);
            metadata.setSource("local-metadata");
            isbnMetadataMapper.insert(metadata);
            return;
        }

        metadata.setTitle(title);
        metadata.setAuthor(author);
        metadata.setCoverImage(coverImage);
        metadata.setSource("local-metadata");
        isbnMetadataMapper.updateById(metadata);
    }

    private void normalizeAndValidate(IsbnMetadata metadata, Long currentId) {
        if (metadata == null) throw new RuntimeException("请求体不能为空");
        String normalized = normalizeIsbn(metadata.getIsbn());
        if (normalized.isBlank()) throw new RuntimeException("ISBN不能为空");
        if (metadata.getTitle() == null || metadata.getTitle().isBlank()) throw new RuntimeException("书名不能为空");

        Long duplicateCount = isbnMetadataMapper.selectCount(new LambdaQueryWrapper<IsbnMetadata>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalized)
                .ne(currentId != null, IsbnMetadata::getId, currentId));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new RuntimeException("ISBN已存在，不能重复维护");
        }

        metadata.setIsbn(normalized);
        metadata.setTitle(metadata.getTitle().trim());
        metadata.setAuthor(blankToNull(metadata.getAuthor()));
        metadata.setPublisher(blankToNull(metadata.getPublisher()));
        metadata.setPublishDate(blankToNull(metadata.getPublishDate()));
        metadata.setCoverImage(blankToNull(metadata.getCoverImage()));
        metadata.setSource(metadata.getSource() == null || metadata.getSource().isBlank()
                ? "local-metadata"
                : metadata.getSource().trim());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeIsbn(String value) {
        return value == null ? "" : value.replace("-", "").replace(" ", "").trim();
    }

    private Map<String, Object> fetchFromBookInfo(String normalized) {
        BookInfo book = bookInfoMapper.selectOne(new LambdaQueryWrapper<BookInfo>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalized)
                .last("LIMIT 1"));
        if (book == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isbn", normalized);
        result.put("title", book.getTitle());
        result.put("author", book.getAuthor());
        result.put("publisher", null);
        result.put("publishDate", null);
        result.put("coverImage", book.getCoverImage());
        result.put("source", "local-book");
        return result;
    }

    private Map<String, Object> fetchFromLocalMetadata(String normalized) {
        IsbnMetadata metadata = isbnMetadataMapper.selectOne(new LambdaQueryWrapper<IsbnMetadata>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", normalized)
                .last("LIMIT 1"));
        if (metadata == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isbn", normalized);
        result.put("title", metadata.getTitle());
        result.put("author", metadata.getAuthor());
        result.put("publisher", metadata.getPublisher());
        result.put("publishDate", metadata.getPublishDate());
        result.put("coverImage", metadata.getCoverImage());
        result.put("source", metadata.getSource() == null ? "local-metadata" : metadata.getSource());
        return result;
    }

    private Map<String, Object> fetchFromOpenLibrary(String normalized, StringBuilder errors) {
        try {
            String bibKey = "ISBN:" + normalized;
            String url = "https://openlibrary.org/api/books?bibkeys="
                    + UriUtils.encode(bibKey, StandardCharsets.UTF_8)
                    + "&format=json&jscmd=data";
            ResponseEntity<String> response = getJson(url);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                appendError(errors, "OpenLibrary Books API返回空内容");
                return null;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode node = root.path(bibKey);
            if (node.isMissingNode() || node.isNull()) {
                appendError(errors, "OpenLibrary Books API未命中");
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("isbn", normalized);
            result.put("title", readText(node, "title"));
            result.put("author", firstAuthor(node.path("authors")));
            result.put("publishDate", readText(node, "publish_date"));
            result.put("publisher", firstPublisher(node.path("publishers")));
            result.put("coverImage", pickCover(node.path("cover")));
            result.put("source", "openlibrary");
            return result;
        } catch (Exception e) {
            appendError(errors, "OpenLibrary请求失败: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> fetchFromOpenLibrarySearch(String normalized, StringBuilder errors) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://openlibrary.org/search.json")
                    .queryParam("isbn", normalized)
                    .queryParam("limit", 1)
                    .queryParam("fields", "title,author_name,publisher,first_publish_year,cover_i,isbn")
                    .build()
                    .toUriString();
            ResponseEntity<String> response = getJson(url);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                appendError(errors, "OpenLibrary Search API返回空内容");
                return null;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode docs = root.path("docs");
            if (!docs.isArray() || docs.isEmpty()) {
                appendError(errors, "OpenLibrary Search API未命中");
                return null;
            }
            JsonNode doc = docs.get(0);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("isbn", normalized);
            result.put("title", readText(doc, "title"));
            result.put("author", firstTextArray(doc.path("author_name")));
            result.put("publishDate", readText(doc, "first_publish_year"));
            result.put("publisher", firstTextArray(doc.path("publisher")));
            result.put("coverImage", openLibraryCoverById(doc.path("cover_i")));
            result.put("source", "openlibrary-search");
            return result;
        } catch (Exception e) {
            appendError(errors, "OpenLibrary Search API请求失败: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> fetchFromGoogleBooks(String normalized, StringBuilder errors) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://www.googleapis.com/books/v1/volumes")
                    .queryParam("q", "isbn:" + normalized)
                    .queryParam("maxResults", 1);
            if (googleApiKey != null && !googleApiKey.isBlank()) {
                builder.queryParam("key", googleApiKey);
            }
            String url = builder.build().toUriString();
            ResponseEntity<String> response = getJson(url);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                appendError(errors, "GoogleBooks返回空内容");
                return null;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                appendError(errors, "GoogleBooks未命中");
                return null;
            }
            JsonNode volumeInfo = items.get(0).path("volumeInfo");
            if (volumeInfo.isMissingNode() || volumeInfo.isNull()) {
                appendError(errors, "GoogleBooks返回缺少volumeInfo");
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("isbn", normalized);
            result.put("title", readText(volumeInfo, "title"));
            result.put("author", firstTextArray(volumeInfo.path("authors")));
            result.put("publishDate", readText(volumeInfo, "publishedDate"));
            result.put("publisher", readText(volumeInfo, "publisher"));
            result.put("coverImage", pickGoogleCover(volumeInfo.path("imageLinks")));
            result.put("source", "googlebooks");
            return result;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 429) {
                appendError(errors, "GoogleBooks配额不足，请配置 Google Books API Key 或稍后再试");
            } else {
                appendError(errors, "GoogleBooks请求失败: HTTP " + e.getStatusCode().value());
            }
            return null;
        } catch (Exception e) {
            appendError(errors, "GoogleBooks请求失败: " + e.getMessage());
            return null;
        }
    }

    private void cacheExternalResult(Map<String, Object> result) {
        String isbn = normalizeIsbn(valueAsString(result.get("isbn")));
        String title = valueAsString(result.get("title"));
        if (isbn == null || isbn.isBlank() || title == null || title.isBlank()) {
            return;
        }
        Long count = isbnMetadataMapper.selectCount(new LambdaQueryWrapper<IsbnMetadata>()
                .apply("REPLACE(REPLACE(isbn, '-', ''), ' ', '') = {0}", isbn));
        if (count != null && count > 0) {
            return;
        }

        IsbnMetadata metadata = new IsbnMetadata();
        metadata.setIsbn(isbn);
        metadata.setTitle(title);
        metadata.setAuthor(valueAsString(result.get("author")));
        metadata.setPublisher(valueAsString(result.get("publisher")));
        metadata.setPublishDate(valueAsString(result.get("publishDate")));
        metadata.setCoverImage(valueAsString(result.get("coverImage")));
        metadata.setSource(valueAsString(result.get("source")));
        isbnMetadataMapper.insert(metadata);
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ResponseEntity<String> getJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "LibraryManage/1.0 (local development)");
        headers.set(HttpHeaders.ACCEPT, "application/json");
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private void appendError(StringBuilder errors, String message) {
        if (errors.length() > 0) {
            errors.append(" | ");
        }
        errors.append(message);
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private String firstAuthor(JsonNode authors) {
        if (!authors.isArray() || authors.isEmpty()) return null;
        JsonNode first = authors.get(0);
        JsonNode name = first.path("name");
        return name.isMissingNode() || name.isNull() ? null : name.asText(null);
    }

    private String firstPublisher(JsonNode publishers) {
        if (!publishers.isArray() || publishers.isEmpty()) return null;
        JsonNode first = publishers.get(0);
        JsonNode name = first.path("name");
        return name.isMissingNode() || name.isNull() ? null : name.asText(null);
    }

    private String firstTextArray(JsonNode values) {
        if (!values.isArray() || values.isEmpty()) return null;
        JsonNode first = values.get(0);
        return first == null || first.isNull() ? null : first.asText(null);
    }

    private String pickCover(JsonNode cover) {
        if (cover == null || cover.isMissingNode() || cover.isNull()) return null;
        if (cover.hasNonNull("large")) return cover.get("large").asText();
        if (cover.hasNonNull("medium")) return cover.get("medium").asText();
        if (cover.hasNonNull("small")) return cover.get("small").asText();
        return null;
    }

    private String openLibraryCoverById(JsonNode coverId) {
        if (coverId == null || coverId.isMissingNode() || coverId.isNull()) return null;
        return "https://covers.openlibrary.org/b/id/" + coverId.asText() + "-L.jpg";
    }

    private String pickGoogleCover(JsonNode cover) {
        if (cover == null || cover.isMissingNode() || cover.isNull()) return null;
        if (cover.hasNonNull("thumbnail")) return cover.get("thumbnail").asText();
        if (cover.hasNonNull("smallThumbnail")) return cover.get("smallThumbnail").asText();
        return null;
    }
}
