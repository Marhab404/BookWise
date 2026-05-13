package com.bookwise.order.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequestForm {

    @NotEmpty(message = "Select at least one book")
    private List<Long> bookIds = new ArrayList<>();
}
