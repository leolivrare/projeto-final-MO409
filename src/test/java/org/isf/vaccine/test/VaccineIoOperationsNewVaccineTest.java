package org.isf.vaccine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        testIoNewVaccineWithParams("1234567890", "Description1", "A", "TypeDescription1", false);
        testIoNewVaccineWithParams("123456890", "Description1", "A", "TypeDescription1", true);
        // Adicione mais chamadas para testIoNewVaccineWithParams aqui com diferentes parâmetros
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