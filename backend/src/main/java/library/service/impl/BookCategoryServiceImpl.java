package library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import library.entity.BookCategory;
import library.mapper.BookCategoryMapper;
import library.service.BookCategoryService;
import org.springframework.stereotype.Service;

@Service
public class BookCategoryServiceImpl extends ServiceImpl<BookCategoryMapper, BookCategory> implements BookCategoryService {
}