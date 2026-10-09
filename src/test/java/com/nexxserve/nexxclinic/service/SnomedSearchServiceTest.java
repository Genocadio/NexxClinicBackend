package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexxserve.nexxclinic.config.NexxIcdProperties;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.SnomedSearchResult;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SnomedSearchServiceTest {

    @Test
    void proxiesSearchToNexxIcdAndReturnsSymptomSuggestions() {
        NexxIcdProperties properties = new NexxIcdProperties();
        properties.setUrl("http://localhost:8082/");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URI.create(
                "http://localhost:8082/api/v1/snomed/search?q=headache&limit=5")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "query":"headache",
                  "limit":5,
                  "total":56,
                  "results":[
                    {
                      "id":"25064002",
                      "preferred":"Headache",
                      "synonyms":["Cephalalgia","Head pain"],
                      "fsn":"Headache (finding)",
                      "type":"finding",
                      "bodySystem":["Neurological finding"],
                      "common":true,
                      "score":1.0
                    }
                  ],
                  "source":"meilisearch",
                  "releaseVersion":"20261001"
                }
                """, MediaType.APPLICATION_JSON));

        SnomedSearchService service = new SnomedSearchService(properties, builder.build());
        ApiResponse<SnomedSearchResult> response = service.search("headache", null, 5);

        assertEquals("SUCCESS", response.status().name());
        assertEquals("25064002", response.data().results().getFirst().id());
        assertEquals("Headache", response.data().results().getFirst().preferred());
        assertEquals("20261001", response.data().releaseVersion());
        server.verify();
    }

    @Test
    void forwardsTheSnomedProvenanceFilter() {
        NexxIcdProperties properties = new NexxIcdProperties();
        properties.setUrl("http://localhost:8082");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URI.create(
                "http://localhost:8082/api/v1/snomed/search?q=pain&limit=10&type=reported")))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"query":"pain","limit":10,"total":1,"results":[],"source":"meilisearch"}
                """, MediaType.APPLICATION_JSON));

        SnomedSearchService service = new SnomedSearchService(properties, builder.build());
        ApiResponse<SnomedSearchResult> response = service.search("pain", "REPORTED", null);

        assertEquals("SUCCESS", response.status().name());
        server.verify();
    }

    @Test
    void reportsMissingUrlAsConfigurationError() {
        SnomedSearchService service = new SnomedSearchService(new NexxIcdProperties());

        ApiResponse<SnomedSearchResult> response = service.search("headache", null, null);

        assertEquals("ERROR", response.status().name());
        assertNull(response.data());
    }

    @Test
    void rejectsUnsupportedProvenanceFilter() {
        NexxIcdProperties properties = new NexxIcdProperties();
        properties.setUrl("http://localhost:8082");

        SnomedSearchService service = new SnomedSearchService(properties);
        ApiResponse<SnomedSearchResult> response = service.search("pain", "symptom", null);

        assertEquals("ERROR", response.status().name());
        assertNull(response.data());
    }
}
