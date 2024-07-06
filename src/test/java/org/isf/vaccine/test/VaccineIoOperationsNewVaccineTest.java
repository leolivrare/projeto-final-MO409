package org.isf.vaccine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.isf.OHCoreTestCase;
import org.isf.utils.exception.OHException;
import org.isf.vaccine.manager.VaccineBrowserManager;
import org.isf.vaccine.model.Vaccine;
import org.isf.vaccine.service.VaccineIoOperationRepository;
import org.isf.vaccine.service.VaccineIoOperations;
import org.isf.vactype.model.VaccineType;
import org.isf.vactype.service.VaccineTypeIoOperationRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;


public class VaccineIoOperationsNewVaccineTest extends OHCoreTestCase {

    private static final Logger LOGGER = Logger.getLogger(VaccineIoOperationsNewVaccineTest.class.getName());

	@Autowired
	VaccineIoOperations vaccineIoOperation;
	@Autowired
	VaccineIoOperationRepository vaccineIoOperationRepository;
	@Autowired
	VaccineTypeIoOperationRepository vaccineTypeIoOperationRepository;
	@Autowired
	VaccineBrowserManager vaccineBrowserManager;


    @Before
	public void setUp() {
		cleanH2InMemoryDb();
	}

    @Test
    public void testIoNewVaccine() throws Exception {
        class Params {
            String vaccineCode;
            String vaccineDescription;
            String vaccineTypeCode;
            String vaccineTypeDescription;
            boolean vaccineAlreadyExists;

            Params(String vaccineCode, String vaccineDescription, String vaccineTypeCode, String vaccineTypeDescription, boolean vaccineAlreadyExists) {
                this.vaccineCode = vaccineCode;
                this.vaccineDescription = vaccineDescription;
                this.vaccineTypeCode = vaccineTypeCode;
                this.vaccineTypeDescription = vaccineTypeDescription;
                this.vaccineAlreadyExists = vaccineAlreadyExists;
            }
        }

        List<Params> paramsList = Arrays.asList(
            new Params("123456890", "Description1", "A", "TypeDescription1", true),
            new Params("1234567890", "Description1", "A", "TypeDescription1", false)
                        // Add more parameters here
        );

        List<Throwable> exceptions = new ArrayList<>();

        for (Params params : paramsList) {
            LOGGER.info("Running test with parameters: " + params.vaccineCode + ", " + params.vaccineDescription + ", " + params.vaccineTypeCode + ", " + params.vaccineTypeDescription + ", " + params.vaccineAlreadyExists);
            try {
                testIoNewVaccineWithParams(params.vaccineCode, params.vaccineDescription, params.vaccineTypeCode, params.vaccineTypeDescription, params.vaccineAlreadyExists);
            } catch (org.junit.ComparisonFailure e) {
                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            AssertionError ae = new AssertionError("There were errors during the tests");
            exceptions.forEach(ae::addSuppressed);
            throw ae;
        }
    }

    private void testIoNewVaccineWithParams(String vaccineCode, String vaccineDescription, String vaccineTypeCode, String vaccineTypeDescription, boolean vaccineAlreadyExists) throws Exception {
        VaccineType vaccineType = new VaccineType();
        vaccineType.setCode(vaccineTypeCode);
        vaccineType.setDescription(vaccineTypeDescription);
        vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);

        // Instantiate a new Vaccine object directly
        Vaccine vaccine = new Vaccine();
        vaccine.setCode(vaccineCode);
        vaccine.setDescription(vaccineDescription);
        vaccine.setVaccineType(vaccineType);

        String checkDescription = vaccineDescription;
        if (vaccineAlreadyExists) {
            checkDescription = "Already Exist Description";
            vaccine.setDescription(checkDescription);
            vaccineIoOperationRepository.saveAndFlush(vaccine);
            vaccine.setDescription(vaccineDescription);
        }
        if (vaccineCode.length() > 10 || vaccineDescription.length() > 50) {
            assertThatThrownBy(() -> {
                vaccineIoOperation.newVaccine(vaccine);
            }).isInstanceOf(OHException.class);
        } else {
            Vaccine result = vaccineIoOperation.newVaccine(vaccine);
            assertThat(result.getCode()).isEqualTo(vaccineCode);
            _checkVaccineIntoDb(vaccine.getCode(), checkDescription);

            // Check if there is more than one record in the vaccine table with the same code
            long count = vaccineIoOperationRepository.findAll().stream()
                .filter(v -> v.getCode().equals(vaccineCode))
                .count();
            assertThat(count).isEqualTo(1);
        }   
    }

    private void _checkVaccineIntoDb(String code, String description) throws OHException {
        Vaccine foundVaccine = vaccineIoOperation.findVaccine(code);

        assertThat(foundVaccine.getCode()).isEqualTo(code);
		assertThat(foundVaccine.getDescription()).isEqualTo(description);
    }
}