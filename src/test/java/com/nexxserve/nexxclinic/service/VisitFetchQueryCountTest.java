package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentProfile;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the visit read path against reintroducing N+1 queries.
 *
 * <p>{@code VisitService.visitsToDtos} is a batch loader: it issues a fixed
 * number of statements regardless of page size. Every DTO mapper it calls has to
 * read from the pre-loaded maps rather than a lazy proxy, otherwise a page of
 * visits silently turns back into hundreds (or thousands) of queries.
 *
 * <p>Before the batch loaders a 10-visit page issued 457 statements and a
 * 100-visit page issued 4417. Now every page issues the same 19 distinct
 * statements and no statement runs more than twice.
 *
 * <p>The assertion is deliberately about the shape of the query set (distinct
 * count and max repeats) rather than an absolute total: the test database is
 * shared with the rest of the suite, so other tests' rows change the exact
 * statement count without indicating an N+1.
 */
@SpringBootTest
class VisitFetchQueryCountTest {

    @Autowired private VisitService visitService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DepartmentProfileRepository departmentProfileRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private VisitDepartmentProductRepository visitDepartmentProductRepository;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private SessionFactory sessionFactory;

    private AuthenticatedUser authUser;

    @BeforeEach
    void setUp() {
        authUser = new AuthenticatedUser(
                UUID.randomUUID(), "admin_query_count",
                Set.of(RoleName.ADMIN), "t", Instant.now().plus(Duration.ofHours(1)));
    }

    @Test
    @Transactional
    void queryCountIsFlatAsPageSizeGrows() {
        // Small page, then a page 10x bigger. No individual statement may scale
        // with the number of visits in the page.
        seed(6);
        Probe small = countStatementsFor(6);
        seed(54);
        Probe large = countStatementsFor(60);

        System.out.println(">>> visits=6  distinct=" + small.distinctQueries()
                + " maxRepeats=" + small.maxRepeatsOfAnyQuery() + " statements=" + small.statements());
        System.out.println(">>> visits=60 distinct=" + large.distinctQueries()
                + " maxRepeats=" + large.maxRepeatsOfAnyQuery() + " statements=" + large.statements());

        assertEquals(small.distinctQueries(), large.distinctQueries(),
                "A 10x larger page issued additional distinct queries - an N+1 crept back in");
        assertTrue(large.maxRepeatsOfAnyQuery() <= 2,
                "A statement ran " + large.maxRepeatsOfAnyQuery()
                        + " times for a 60-visit page; nothing in visitsToDtos may scale with page size");
    }

    private record Probe(int statements, int distinctQueries, long maxRepeatsOfAnyQuery) {
    }

    private Probe countStatementsFor(int pageSize) {
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            ApiResponse<List<com.nexxserve.nexxclinic.dto.out.VisitDto>> res = visitService.visits(
                    new SearchVisitsInput(null, null, null, null, null, null, null, 0, pageSize),
                    authUser);
            assertTrue(res.data() != null && res.data().size() == pageSize,
                    "Expected " + pageSize + " visits in the page");

            int distinct = 0;
            long maxRepeats = 0;
            for (String raw : stats.getQueries()) {
                distinct++;
                maxRepeats = Math.max(maxRepeats, stats.getQueryStatistics(raw).getExecutionCount());
            }
            return new Probe((int) stats.getPrepareStatementCount(), distinct, maxRepeats);
        } finally {
            stats.setStatisticsEnabled(false);
        }
    }

    /**
     * Builds visits shaped like real clinical data — 3 departments each with an
     * applied profile, 4 product lines, and worker references on the department
     * and every product (the shape that used to trigger the N+1).
     */
    private void seed(int visitCount) {
        DepartmentProfile profile = new DepartmentProfile();
        profile.setName("Standard-" + UUID.randomUUID().toString().substring(0, 6));
        profile.setEncounterType(EncounterType.OUTPATIENT);
        profile.setDefault(false);

        // uk_visit_department is (visit_id, department_id), so a visit cannot
        // repeat a department — one department per slot.
        List<Department> depts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Department d = new Department();
            d.setName("Dept" + i + "-" + UUID.randomUUID().toString().substring(0, 6));
            depts.add(departmentRepository.save(d));
            if (i == 0) {
                profile.setDepartment(d);
                departmentProfileRepository.save(profile);
            }
        }

        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Worker w = new Worker();
            w.setFirstName("W" + i);
            w.setLastName("Test");
            w.setEmail("wq" + UUID.randomUUID().toString().substring(0, 8) + "@x.com");
            w.setPhoneNumber("+2507880" + (100000 + (int) (Math.random() * 899999)));
            w.setUsername("wq" + UUID.randomUUID().toString().substring(0, 8));
            w.setGender(Gender.MALE);
            w.setDateOfBirth(LocalDate.of(1990, 1, 1));
            w.setAccountStatus(AccountStatus.ACTIVE);
            w.setActive(true);
            w.setMaxActiveSessions(1);
            workers.add(workerRepository.save(w));
        }

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Product p = new Product();
            p.setName("Prod-" + UUID.randomUUID().toString().substring(0, 8));
            p.setGenericName("gen" + i);
            p.setCode("C" + UUID.randomUUID().toString().substring(0, 8));
            p.setType(ProductType.MEDICAL_ACT);
            p.setUnit(ProductUnit.PIECE);
            p.setMetadata("{}");
            p.setNotPaid(false);
            products.add(productRepository.save(p));
        }

        for (int v = 0; v < visitCount; v++) {
            Patient patient = new Patient();
            patient.setFirstName("P" + v);
            patient.setLastName("Q" + UUID.randomUUID().toString().substring(0, 4));
            patient.setFullName(patient.getFirstName() + " " + patient.getLastName());
            patient.setPatientIdentifier("P-QC-" + UUID.randomUUID().toString().substring(0, 8));
            patient.setPrimaryPhoneNumber("+250789" + (100000 + (int) (Math.random() * 899999)));
            patient.setGender(Gender.MALE);
            patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
            patientRepository.save(patient);

            Visit visit = new Visit();
            visit.setPatient(patient);
            visit.setStatus(VisitStatus.CREATED);
            visit.setVisitDate(LocalDateTime.now().minusSeconds(v));
            visitRepository.save(visit);

            for (int d = 0; d < 3; d++) {
                VisitDepartment vd = new VisitDepartment();
                vd.setVisit(visit);
                vd.setDepartment(depts.get(d));
                vd.setProfile(profile);
                vd.setStatus(VisitDepartmentStatus.ACTIVE);
                vd.setEncounterType(EncounterType.OUTPATIENT);
                vd.setAddedBy(workers.get(d % workers.size()));
                vd.setCompletedBy(workers.get((d + 1) % workers.size()));
                vd.getProcessors().add(workers.get(d % workers.size()));
                visitDepartmentRepository.save(vd);

                for (int pi = 0; pi < 4; pi++) {
                    VisitDepartmentProduct pdp = new VisitDepartmentProduct();
                    pdp.setVisitDepartment(vd);
                    pdp.setProduct(products.get(pi % products.size()));
                    pdp.setQuantity(java.math.BigDecimal.ONE);
                    pdp.setStatus(VisitProductStatus.PENDING);
                    pdp.setAddedBy(workers.get(pi % workers.size()));
                    visitDepartmentProductRepository.save(pdp);
                }
            }
        }
        entityManager.flush();
        entityManager.clear();
    }
}
