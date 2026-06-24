package com.bookwise.admin.dto;

import java.util.List;

public record BarSeries(String label, List<String> labels, List<Long> values) {
}
