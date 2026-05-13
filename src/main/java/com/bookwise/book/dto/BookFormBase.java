package com.bookwise.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public abstract class BookFormBase {

    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;

    private String description;

    private String isbn;

    @NotNull(message = "Publication year is required")
    @Min(value = 1000, message = "Enter a valid publication year")
    private Integer publicationYear;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull(message = "Price is required")
    private BigDecimal priceAmount;

    @NotBlank(message = "Currency is required")
    private String currency = "MAD";

    @NotNull(message = "Published flag is required")
    private Boolean published = Boolean.FALSE;

    @NotEmpty(message = "Select at least one author")
    private List<Long> authorIds = new ArrayList<>();

    @NotEmpty(message = "Select at least one category")
    private List<Long> categoryIds = new ArrayList<>();

    private MultipartFile coverImage;

    private MultipartFile[] digitalFiles = new MultipartFile[0];
}
