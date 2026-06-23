package com.bookwise.chatbot.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTest {

    private GeminiClient geminiClient;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("https://generativelanguage.googleapis.com").build();
        geminiClient = new GeminiClient(restClient, "gemini-3.1-flash-lite");
    }

    @Test
    void testPostInteractionSuccess() throws Exception {
        String responseJson = """
            {
              "id": "interaction-123",
              "status": "completed",
              "steps": [
                {
                  "type": "thought",
                  "content": []
                },
                {
                  "type": "model_output",
                  "content": [
                    {
                      "type": "text",
                      "text": "Hello, I am Gemini!"
                    }
                  ]
                }
              ]
            }
            """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/interactions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        GeminiClient.InteractionResponse response = geminiClient.postInteraction("Hi", null);
        assertNotNull(response);
        assertEquals("interaction-123", response.id());
        assertEquals("completed", response.status());
        
        String extracted = GeminiClient.extractTextResponse(response);
        assertEquals("Hello, I am Gemini!", extracted);

        mockServer.verify();
    }
}
