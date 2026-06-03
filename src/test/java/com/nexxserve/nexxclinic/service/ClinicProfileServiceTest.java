package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.graphql.input.ClinicContactInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateClinicProfileInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.ClinicContactType;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
@SuppressWarnings("unchecked")
class ClinicProfileServiceTest {

    @Autowired
    private ClinicProfileService clinicProfileService;

    @Autowired
    private ClinicProfileRepository clinicProfileRepository;

    @Test
    void updateClinicProfileStoresTypedContactList() {
        UpdateClinicProfileInput input = new UpdateClinicProfileInput(
                "Nexx Clinic",
                "Plot 12",
                null,
                null,
                List.of(
                        new ClinicContactInput(ClinicContactType.PHONE, "+255700000001", "support"),
                        new ClinicContactInput(ClinicContactType.EMAIL, "finance@nexxclinic.com", "finance"),
                        new ClinicContactInput(ClinicContactType.POBOX, "P.O. Box 123", "head office")
                ),
                null
        );

        ApiResponse response = clinicProfileService.updateClinicProfile(input);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertEquals("Nexx Clinic", data.get("name"));

        List<Map<String, Object>> contacts = (List<Map<String, Object>>) data.get("contacts");
        assertEquals(3, contacts.size());
        assertEquals("PHONE", contacts.get(0).get("contactType"));
        assertEquals("+255700000001", contacts.get(0).get("value"));
        assertEquals("support", contacts.get(0).get("description"));
        assertEquals("EMAIL", contacts.get(1).get("contactType"));
        assertEquals("POBOX", contacts.get(2).get("contactType"));

        ClinicProfile savedProfile = clinicProfileRepository.findFirstByOrderByCreatedAtAsc().orElseThrow();
        assertEquals(3, savedProfile.getContacts().size());
        assertEquals(ClinicContactType.PHONE, savedProfile.getContacts().get(0).getContactType());
        assertEquals("support", savedProfile.getContacts().get(0).getDescription());
    }
}