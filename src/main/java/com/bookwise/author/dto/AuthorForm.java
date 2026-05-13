package com.bookwise.author.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorForm {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String biography;
}
