package com.bookwise.category.entity;

import com.bookwise.book.entity.Book;
import com.bookwise.common.AbstractAuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends AbstractAuditedEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "text")
    private String description;

    @ManyToMany(mappedBy = "categories")
    private List<Book> books = new ArrayList<>();
}
