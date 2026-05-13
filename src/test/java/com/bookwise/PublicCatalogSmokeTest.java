package com.bookwise;

import com.bookwise.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicCatalogSmokeTest extends IntegrationTestSupport {

    @Test
    void homepageAndCatalogRenderPublishedBooks() throws Exception {
        var visibleBook = createBook("Visible Smoke Book", 1990L, true);
        createBook("Hidden Smoke Book", 2990L, false);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visible Smoke Book")))
                .andExpect(content().string(not(containsString("Hidden Smoke Book"))));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visible Smoke Book")))
                .andExpect(content().string(not(containsString("Hidden Smoke Book"))));

        mockMvc.perform(get("/books/" + visibleBook.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visible Smoke Book")));
    }
}
