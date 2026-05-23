package library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import library.entity.IsbnMetadata;

import java.util.Map;

public interface IsbnMetadataService {
    Page<IsbnMetadata> getPage(Page<IsbnMetadata> page, String keyword, String source);
    IsbnMetadata getById(Long id);
    void createMetadata(IsbnMetadata metadata);
    void updateMetadata(Long id, IsbnMetadata metadata);
    void deleteMetadata(Long id);
    Map<String, Object> fetchByIsbn(String isbn);
    void saveLocalMetadata(String isbn, String title, String author, String coverImage);
}
