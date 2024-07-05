package org.isf.vaccine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.PostConstruct;

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
import org.springframework.boot.test.context.SpringBootTest;


public class VaccineIoOperationsNewVaccineTest extends OHCoreTestCase {

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
        String vaccineCode = "1234567890";
        String vaccineDescription = "Description1";
        String vaccineTypeCode = "A";
        String vaccineTypeDescription = "TypeDescription1";
        boolean vaccineAlreadyExists = false;
        VaccineType vaccineType = new VaccineType();
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
            _checkVaccineIntoDb(vaccine.getCode(), vaccine.getDescription());
        }
    }

    private void _checkVaccineIntoDb(String code, String description) throws OHException {
        Vaccine foundVaccine = vaccineIoOperation.findVaccine(code);

        assertThat(foundVaccine.getCode()).isEqualTo(code);
		assertThat(foundVaccine.getDescription()).isEqualTo(description);
    }
}