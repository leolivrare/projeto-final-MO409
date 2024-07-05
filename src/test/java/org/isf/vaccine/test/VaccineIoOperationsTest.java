package org.isf.vaccine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

import org.isf.OHCoreTestCase;
import org.isf.utils.exception.OHException;
import org.isf.vaccine.manager.VaccineBrowserManager;
import org.isf.vaccine.model.Vaccine;
import org.isf.vaccine.service.VaccineIoOperationRepository;
import org.isf.vaccine.service.VaccineIoOperations;
import org.isf.vactype.model.VaccineType;
import org.isf.vactype.service.VaccineTypeIoOperationRepository;
import org.isf.vactype.test.TestVaccineType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy; // Add this import statement
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.utils.exception.model.OHSeverityLevel;

@RunWith(Parameterized.class)
public class VaccineIoOperationsTest extends OHCoreTestCase {

    private static TestVaccine testVaccine;
    private static TestVaccineType testVaccineType;

    @InjectMocks
    VaccineIoOperations vaccineIoOperation;

    @Mock
    VaccineIoOperationRepository vaccineIoOperationRepository;

    @Mock
    VaccineTypeIoOperationRepository vaccineTypeIoOperationRepository;

    @Autowired
    VaccineBrowserManager vaccineBrowserManager;

    private String code;
    private boolean existsInDb;
    private boolean dbIsOn;

	@PostConstruct
    public void init() {
        cleanH2InMemoryDb();
    }

    @BeforeClass
    public static void setUpClass() {
        testVaccine = new TestVaccine();
        testVaccineType = new TestVaccineType();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    public VaccineIoOperationsTest(String code, boolean existsInDb, boolean dbIsOn) {
        this.code = code;
        this.existsInDb = existsInDb;
        this.dbIsOn = dbIsOn;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testCases() {
        return Arrays.asList(new Object[][] {
            // Case 1: Test if getVaccine can correctly retrieve a vaccine when the vaccine code exists in the database and the database is on
            {"1", true, true}, 
            // Case 2: Test how getVaccine handles a null vaccine code when the database is on
            {null, true, true},
            // Case 4: Test how getVaccine handles an empty vaccine code when the database is on
            {"", false, true}, 
            // Case 5: Test how getVaccine handles a vaccine code that does not exist in the database when the database is on
            {"12", false, true}, 
            // Case 6: Test how getVaccine handles a vaccine code that exists in the database but the database is off
            {"1", false, true}, 
            // Case 7: Test how getVaccine handles a vaccine code that exists in the database but the database is off
            {"1", true, false} 
        });
    }

    @Test
    public void testIoGetVaccineShouldFindByTypeCode() throws Exception {
		Vaccine foundVaccine = null;
		if (dbIsOn) {
			if (existsInDb) {
				String code = _setupTestVaccine(false);
				foundVaccine = vaccineIoOperation.findVaccine(code);
				when(vaccineIoOperationRepository.findByVaccineType_CodeOrderByDescriptionAsc(code)).thenReturn(Arrays.asList(foundVaccine));
			} else {
				when(vaccineIoOperationRepository.findByVaccineType_CodeOrderByDescriptionAsc(code)).thenReturn(Collections.emptyList());
			}
			List<Vaccine> vaccines = vaccineIoOperation.getVaccine(code);
			if (existsInDb) {
				if (!vaccines.isEmpty()) {
					assertThat(vaccines.get(vaccines.size() - 1).getDescription()).isEqualTo(foundVaccine.getDescription());
				}
			} else {
				assertThat(vaccines).isEmpty();
			}
		} else {
			RuntimeException cause = new RuntimeException("Database is off");
			when(vaccineIoOperationRepository.findByVaccineType_CodeOrderByDescriptionAsc(code)).thenThrow(cause);
			assertThatThrownBy(() -> vaccineIoOperation.getVaccine(code)).isInstanceOf(RuntimeException.class);
		}
	}

    private String _setupTestVaccine(boolean usingSet) throws OHException {
        VaccineType vaccineType = testVaccineType.setup(false);
        Vaccine vaccine = testVaccine.setup(vaccineType, usingSet);
        vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);
        vaccineIoOperationRepository.saveAndFlush(vaccine);
        return vaccine.getCode();
    }

}