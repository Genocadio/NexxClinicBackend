package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ClinicProfileDto;
import com.nexxserve.nexxclinic.dto.out.ClinicContactDto;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.graphql.input.ClinicContactInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateClinicProfileInput;
import com.nexxserve.nexxclinic.model.ClinicContactType;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ClinicProfileServiceTest {

    @Autowired
    private ClinicProfileService clinicProfileService;

    @Autowired
    private ClinicProfileRepository clinicProfileRepository;

    @Test
    void updateClinicProfileStoresTypedContactList() {

        UpdateClinicProfileInput input = new UpdateClinicProfileInput(
                "Nexx Clinic",
                null,
                "Plot 12",
                null,
                "TIN-12345",
                List.of(
                        new ClinicContactInput(ClinicContactType.PHONE, "+255700000001", "support"),
                        new ClinicContactInput(ClinicContactType.EMAIL, "finance@nexxclinic.com", "finance"),
                        new ClinicContactInput(ClinicContactType.POBOX, "P.O. Box 123", "head office")
                ),
                null
        );

        ApiResponse<ClinicProfileDto> response =
                clinicProfileService.updateClinicProfile(input);

        // ======================
        // BASIC ASSERTIONS
        // ======================
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        ClinicProfileDto data = response.data();

        assertEquals("Nexx Clinic", data.name());
        assertEquals(3, data.contacts().size());

        // ======================
        // CONTACT ASSERTIONS
        // ======================
        ClinicContactDto phone = data.contacts().get(0);
        ClinicContactDto email = data.contacts().get(1);
        ClinicContactDto pobox = data.contacts().get(2);

        assertEquals(ClinicContactType.PHONE, phone.contactType());
        assertEquals("+255700000001", phone.value());
        assertEquals("support", phone.description());

        assertEquals(ClinicContactType.EMAIL, email.contactType());
        assertEquals("finance@nexxclinic.com", email.value());

        assertEquals(ClinicContactType.POBOX, pobox.contactType());

        // ======================
        // DB ASSERTION
        // ======================
        ClinicProfile savedProfile =
                clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
                        .orElseThrow();

        assertEquals(3, savedProfile.getContacts().size());
        assertEquals(ClinicContactType.PHONE, savedProfile.getContacts().get(0).getContactType());
        assertEquals("support", savedProfile.getContacts().get(0).getDescription());
    }
}
