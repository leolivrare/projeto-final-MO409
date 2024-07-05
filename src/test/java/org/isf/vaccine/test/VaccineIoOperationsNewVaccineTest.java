package org.isf.vaccine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

@RunWith(Parameterized.class)
public class VaccineIoOperationsNewVaccineTest extends OHCoreTestCase {

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

    private String vaccineCode;
    private String vaccineDescription;
    private String vaccineTypeCode;
    private String vaccineTypeDescription;
    private boolean vaccineAlreadyExists;
    private boolean dbConnection;

    public VaccineIoOperationsNewVaccineTest(String vaccineCode, String vaccineDescription, String vaccineTypeCode, String vaccineTypeDescription, boolean vaccineAlreadyExists, boolean dbConnection) {
        this.vaccineCode = vaccineCode;
        this.vaccineDescription = vaccineDescription;
        this.vaccineTypeCode = vaccineTypeCode;
        this.vaccineTypeDescription = vaccineTypeDescription;
        this.vaccineAlreadyExists = vaccineAlreadyExists;
        this.dbConnection = dbConnection;
    }

    @BeforeClass
    public static void setUpClass() {
        testVaccine = new TestVaccine();
        testVaccineType = new TestVaccineType();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        vaccineIoOperation = new VaccineIoOperations();
        vaccineIoOperationRepository = Mockito.mock(VaccineIoOperationRepository.class);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> testCases() {
        return Arrays.asList(new Object[][] {
            {"1234567890", "Description1", "A", "TypeDescription1", false, true}
            // {"12345678901", "Description2", "B", "TypeDescription2", false, true},
            // {"12345", "Description3", "C", "TypeDescription3", true, true},
            // {"1234567890", "Description4", "D", "TypeDescription4", false, true},
            // {"1234567890", "Description5", "E", "TypeDescription5", false, false}
        });
    }

    @Test
    public void testIoNewVaccine() throws Exception {
        if (dbConnection) {
            VaccineType vaccineType = testVaccineType.setup(false);
            vaccineType.setCode(vaccineTypeCode);
            vaccineType.setDescription(vaccineTypeDescription);
            vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);
    
            // Instantiate a new Vaccine object directly
            Vaccine vaccine = new Vaccine();
            vaccine.setCode(vaccineCode);
            vaccine.setDescription(vaccineDescription);
            vaccine.setVaccineType(vaccineType);
    
            if (vaccineAlreadyExists) {
                vaccineIoOperationRepository.saveAndFlush(vaccine);
            }
            if (vaccineCode.length() > 10 || vaccineDescription.length() > 50) {
                assertThatThrownBy(() -> {
                    vaccineIoOperation.newVaccine(vaccine);
                }).isInstanceOf(OHException.class);
            } else {
                Vaccine result = vaccineIoOperation.newVaccine(vaccine);
                assertThat(result.getCode()).isEqualTo(vaccineCode);
                _checkVaccineIntoDb(vaccine.getCode());
            }
        } else {
            assertThatThrownBy(() -> {
                vaccineIoOperation.newVaccine(new Vaccine());
            }).isInstanceOf(OHException.class);
        }
    }

    private void _checkVaccineIntoDb(String code) throws OHException {
        Vaccine foundVaccine = vaccineIoOperation.findVaccine(code);
        testVaccine.check(foundVaccine);
    }
}