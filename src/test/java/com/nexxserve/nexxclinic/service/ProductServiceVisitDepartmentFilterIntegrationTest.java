package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.ProductDto;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.graphql.input.SearchProductsInput;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProductServiceVisitDepartmentFilterIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private VisitDepartmentProductRepository visitDepartmentProductRepository;

    @Test
    void searchProducts_filtersOutAlreadyExistingProductsInVisitDepartment() {
        // 1. Create two products
        Product productA = new Product();
        productA.setName("Paracetamol Test " + UUID.randomUUID());
        productA.setCode("CODE-" + UUID.randomUUID().toString().substring(0, 8));
        productA.setType(ProductType.DRUG);
        productA.setUnit(ProductUnit.TABLET);
        productA.setClinicPrice(new BigDecimal("500.00"));
        productA = productRepository.saveAndFlush(productA);

        Product productB = new Product();
        productB.setName("Amoxicillin Test " + UUID.randomUUID());
        productB.setCode("CODE-" + UUID.randomUUID().toString().substring(0, 8));
        productB.setType(ProductType.DRUG);
        productB.setUnit(ProductUnit.CAPSULE);
        productB.setClinicPrice(new BigDecimal("1200.00"));
        productB = productRepository.saveAndFlush(productB);

        // 2. Create Patient, Department, Visit, VisitDepartment
        Patient patient = new Patient();
        patient.setPatientIdentifier("PAT-" + UUID.randomUUID().toString().substring(0, 8));
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setFullName("John Doe");
        patient.setGender(Gender.MALE);
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient = patientRepository.saveAndFlush(patient);

        Department dept = new Department();
        dept.setName("Dental Department " + UUID.randomUUID().toString().substring(0, 8));
        dept = departmentRepository.saveAndFlush(dept);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.saveAndFlush(visit);

        VisitDepartment visitDept = new VisitDepartment();
        visitDept.setVisit(visit);
        visitDept.setDepartment(dept);
        visitDept.setStatus(VisitDepartmentStatus.ACTIVE);
        visitDept.setEncounterType(EncounterType.OUTPATIENT);
        visitDept = visitDepartmentRepository.saveAndFlush(visitDept);

        // 3. Add Product A to visit department
        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(visitDept);
        vdp.setProduct(productA);
        vdp.setQuantity(new BigDecimal("1.0"));
        vdp.setStatus(VisitProductStatus.PENDING);
        vdp.setSource(VisitDepartmentProductSource.USER);
        vdp.setDeleted(false);
        visitDepartmentProductRepository.saveAndFlush(vdp);

        // 4. Search with visitDepartmentId -> Product A must be excluded, Product B included
        SearchProductsInput inputWithDept = new SearchProductsInput(
                null,
                null,
                0,
                100,
                null,
                visitDept.getId()
        );
        ApiResponse<List<ProductDto>> responseWithDept = productService.products(inputWithDept);
        assertNotNull(responseWithDept);
        assertNotNull(responseWithDept.data());

        List<UUID> returnedIds = responseWithDept.data().stream().map(ProductDto::id).toList();
        assertFalse(returnedIds.contains(productA.getId()), "Product A should be filtered out because it is already on the visit department");
        assertTrue(returnedIds.contains(productB.getId()), "Product B should be returned");

        // 5. Search without visitDepartmentId -> Both products returned
        SearchProductsInput inputWithoutDept = new SearchProductsInput(
                null,
                null,
                0,
                100,
                null,
                null
        );
        ApiResponse<List<ProductDto>> responseWithoutDept = productService.products(inputWithoutDept);
        assertNotNull(responseWithoutDept);
        assertNotNull(responseWithoutDept.data());
        List<UUID> allReturnedIds = responseWithoutDept.data().stream().map(ProductDto::id).toList();
        assertTrue(allReturnedIds.contains(productA.getId()), "Product A should be present when visitDepartmentId is not supplied");
        assertTrue(allReturnedIds.contains(productB.getId()), "Product B should be present when visitDepartmentId is not supplied");
    }
}
