package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexxserve.nexxclinic.config.NexxIcdProperties;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.Icd11SearchResult;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class Icd11SearchServiceTest {

    @Test
    void proxiesSearchToNexxIcdAndReturnsSuggestions() {
        NexxIcdProperties properties = new NexxIcdProperties();
        properties.setUrl("http://localhost:8082/");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URI.create(
                "http://localhost:8082/api/v1/search?q=malaria&lang=en&limit=5&chapter=01")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "query":"malaria",
                  "lang":"en",
                  "limit":5,
                  "total":1,
                  "results":[
                    {"id":"http://id.who.int/icd/entity/123","code":"1A00","title":"Malaria","chapter":"01","score":97.6}
                  ],
                  "source":"meilisearch"
                }
                """, MediaType.APPLICATION_JSON));

        Icd11SearchService service = new Icd11SearchService(properties, builder.build());
        ApiResponse<Icd11SearchResult> response = service.search("malaria", null, 5, "01");

        assertEquals("SUCCESS", response.status().name());
        assertEquals("1A00", response.data().results().getFirst().code());
        assertEquals("Malaria", response.data().results().getFirst().title());
        server.verify();
    }

    @Test
    void reportsMissingUrlAsConfigurationError() {
        Icd11SearchService service = new Icd11SearchService(
            new NexxIcdProperties()
        );

        ApiResponse<Icd11SearchResult> response = service.search("malaria", null, null, null);

        assertEquals("ERROR", response.status().name());
        assertNull(response.data());
    }
}
