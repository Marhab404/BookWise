package com.bookwise.book.entity;

import com.bookwise.author.entity.Author;
import com.bookwise.category.entity.Category;
import com.bookwise.common.AbstractAuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "books",
        indexes = {
                @Index(name = "idx_books_published", columnList = "published"),
                @Index(name = "idx_books_isbn", columnList = "isbn")
        }
)
public class Book extends AbstractAuditedEntity {

    @Column(nullable = false)
    private String title;

    private String subtitle;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "text")
    private String description;

    @Column(unique = true, length = 50)
    private String isbn;

    private Integer publicationYear;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(length = 500)
    private String coverImagePath;

    @Column(nullable = false)
    private Long priceAmount;

    @Column(nullable = false, length = 3)
    private String currency = "MAD";

    @Column(nullable = false)
    private boolean published = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @OrderBy("fullName ASC")
    private Set<Author> authors = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @OrderBy("name ASC")
    private Set<Category> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "book", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<BookFile> files = new ArrayList<>();
}
