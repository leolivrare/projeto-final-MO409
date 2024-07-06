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

public class VaccineIoOperationsFunctionalTest extends OHCoreTestCase {

    private static final Logger LOGGER = Logger.getLogger(VaccineIoOperationsFunctionalTest.class.getName());

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
        cleanMySQLDb();
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
            /* Classe Válida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - vaccineDescription: string com 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina ainda não existe no banco de dados
            * Resultado esperado: A função newVaccine deve ser concluída com sucesso e o item deve ser inserido no banco de dados */
            new Params("1234567890", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), false),

            /* Classe Inválida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - vaccineDescription: string com 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina já existe no banco de dados
            * Resultado esperado: A função newVaccine deve falhar, pois o item já existe no banco de dados
            * Erro Encontrado: a função newVaccine não quebra ao tentar inserir um item que já existe.
            * Ao invés disso, ela atualiza o item já existente. Isso resulta num problema de integridade de dados,
            * pois perdemos as informações do item já existente. */
            new Params("0000000000", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), true),
            
            /* Classe Inválida 2:
            *  - vaccineCode: string com 11 caracteres
            *  - vaccineDescription: string com 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina ainda não existe no banco de dados
            * Resultado esperado: A função newVaccine deve falhar por tentar adicionar o campo vaccineCode no banco,
            * mas existe uma constraint que não permite string maior que 10 caracteres. */
            new Params("00000000001", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), false),

            /* Classe Inválida 3:
            *  - vaccineCode: string com 10 caracteres
            *  - vaccineDescription: string com mais de 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina ainda não existe no banco de dados
            * Resultado esperado: A função newVaccine deve falhar por tentar adicionar o campo vaccineDescription no banco,
            * mas existe uma constraint que não permite string maior que 50 caracteres. */
            new Params("1111111111", String.format("%-51s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), false)
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
            }).isInstanceOf(org.isf.utils.exception.OHDataIntegrityViolationException.class);
        } else {
            Vaccine result = vaccineIoOperation.newVaccine(vaccine);
            assertThat(result.getCode()).isEqualTo(vaccineCode);
            _checkVaccineIntoDb(vaccine.getCode(), checkDescription);

            long count = vaccineIoOperationRepository.findAll().stream()
                .filter(v -> v.getCode().equals(vaccineCode))
                .count();
            assertThat(count).isEqualTo(1);
        }   
    }

    private void _checkVaccineIntoDb(String code, String description) throws OHException {
        Vaccine foundVaccine = vaccineIoOperation.findVaccine(code);
        LOGGER.info("Found vaccine: " + foundVaccine.getCode());
        assertThat(foundVaccine.getCode()).isEqualTo(code);
		assertThat(foundVaccine.getDescription()).isEqualTo(description);
    }
}