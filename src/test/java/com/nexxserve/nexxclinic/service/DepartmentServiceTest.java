package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.graphql.input.SearchDepartmentsInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DepartmentServiceTest {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void testDepartmentsFiltersSupportRequestsAndRequestsProducts() {
        Department deptA = new Department();
        deptA.setName("Dept A");
        deptA.setSupportRequests(true);
        deptA.setRequestsProducts(true);
        departmentRepository.save(deptA);

        Department deptB = new Department();
        deptB.setName("Dept B");
        deptB.setSupportRequests(false);
        deptB.setRequestsProducts(true);
        departmentRepository.save(deptB);

        Department deptC = new Department();
        deptC.setName("Dept C");
        deptC.setSupportRequests(true);
        deptC.setRequestsProducts(false);
        departmentRepository.save(deptC);

        ApiResponse allResponse = departmentService.departments(new SearchDepartmentsInput(null, null, null, 0, 10));
        assertEquals(ResponseStatus.SUCCESS, allResponse.status());
        assertNotNull(allResponse.data());
        assertEquals(3, ((List<?>) allResponse.data()).size());

        ApiResponse supportOnly = departmentService.departments(new SearchDepartmentsInput(null, true, null, 0, 10));
        assertEquals(ResponseStatus.SUCCESS, supportOnly.status());
        assertEquals(2, ((List<?>) supportOnly.data()).size());

        ApiResponse productsOnly = departmentService.departments(new SearchDepartmentsInput(null, null, false, 0, 10));
        assertEquals(ResponseStatus.SUCCESS, productsOnly.status());
        assertEquals(1, ((List<?>) productsOnly.data()).size());

        ApiResponse bothFilters = departmentService.departments(new SearchDepartmentsInput(null, true, true, 0, 10));
        assertEquals(ResponseStatus.SUCCESS, bothFilters.status());
        assertEquals(1, ((List<?>) bothFilters.data()).size());

        List<?> bothData = (List<?>) bothFilters.data();
        assertTrue(bothData.get(0) instanceof Map);
    }
}
