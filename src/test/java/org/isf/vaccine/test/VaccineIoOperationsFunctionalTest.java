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
import org.isf.vactype.test.TestVaccineType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.logging.Logger;

public class VaccineIoOperationsFunctionalTest extends OHCoreTestCase {

    private static TestVaccineType testVaccineType;

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


    @Test
    public void testIoUpdateVaccine() throws Exception {
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
            *  - Vacina existe no banco de dados
            * Resultado esperado: A função updateVaccine deve ser concluída com sucesso e o item deve ser atualizado no banco de dados */
            new Params("1234567890", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), true),

            /* Classe Inválida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - vaccineDescription: string com mais de 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina existe no banco de dados
            * Resultado esperado: A função updateVaccine deve falhar, pois o vaccineDescription é maior do que o permitido */
            new Params("1111111111", String.format("%-51s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), true),

            /* Classe Inválida 2:
            *  - vaccineCode: string com 10 caracteres
            *  - vaccineDescription: string com 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função updateVaccine deve falhar, pois vai tentar fazer o update de um item que não existe no banco 
            * Erro encontrado: A função updateVaccine não falha ao tentar fazer update de um item que não existe no banco. Ao invés disso,
            * ela insere o item como se fosse novo. Isso pode gerar problema de integridade de dados, pois a função de update está funcionando
            * também como insert.*/
            new Params("2222222222", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), false)
        );

        List<Throwable> exceptions = new ArrayList<>();

        for (Params params : paramsList) {
            LOGGER.info("Running test with parameters: " + params.vaccineCode + ", " + params.vaccineDescription + ", " + params.vaccineTypeCode + ", " + params.vaccineTypeDescription + ", " + params.vaccineAlreadyExists);
            try {
                testIoUpdateVaccineWithParams(params.vaccineCode, params.vaccineDescription, params.vaccineTypeCode, params.vaccineTypeDescription, params.vaccineAlreadyExists);
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
    

    private void testIoUpdateVaccineWithParams(String vaccineCode, String vaccineDescription, String vaccineTypeCode, String vaccineTypeDescription, boolean vaccineAlreadyExists) throws Exception {
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
                vaccineIoOperation.updateVaccine(vaccine);
            }).isInstanceOf(org.isf.utils.exception.OHDataIntegrityViolationException.class);
        } else {
            if (!vaccineAlreadyExists) {
                assertThatThrownBy(() -> {
                    vaccineIoOperation.updateVaccine(vaccine);
                }).isInstanceOf(org.isf.utils.exception.OHException.class);
            } else {
                Vaccine result = vaccineIoOperation.updateVaccine(vaccine);
                assertThat(result.getCode()).isEqualTo(vaccineCode);
                _checkVaccineIntoDb(vaccine.getCode(), vaccineDescription);

                long count = vaccineIoOperationRepository.findAll().stream()
                    .filter(v -> v.getCode().equals(vaccineCode))
                    .count();
                assertThat(count).isEqualTo(1);
            }
        }   
    }

    @Test
    public void testIoDeleteVaccine() throws Exception {
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
            *  - Vacina existe no banco de dados
            * Resultado esperado: A função deleteVaccine deve ser concluída com sucesso e o item deve ser removido do banco de dados */
            new Params("1234567890", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), true),


            /* Classe Inválida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - vaccineDescription: string com 50 caracteres
            *  - vaccineTypeCode: string com 1 caractere
            *  - vaccineTypeDescription: string com 50 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função deleteVaccine deve ser concluída com sucesso e não deve alterar nada no banco.*/
            new Params("2222222222", String.format("%-50s", "Description").replace(' ', 'a'), "A", String.format("%-50s", "TypeDescription").replace(' ', 'a'), false)
        );

        List<Throwable> exceptions = new ArrayList<>();

        for (Params params : paramsList) {
            LOGGER.info("Running test with parameters: " + params.vaccineCode + ", " + params.vaccineDescription + ", " + params.vaccineTypeCode + ", " + params.vaccineTypeDescription + ", " + params.vaccineAlreadyExists);
            try {
                testIoDeleteVaccineWithParams(params.vaccineCode, params.vaccineDescription, params.vaccineTypeCode, params.vaccineTypeDescription, params.vaccineAlreadyExists);
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

    private void testIoDeleteVaccineWithParams(String vaccineCode, String vaccineDescription, String vaccineTypeCode, String vaccineTypeDescription, boolean vaccineAlreadyExists) throws Exception {
        VaccineType vaccineType = new VaccineType();
        vaccineType.setCode(vaccineTypeCode);
        vaccineType.setDescription(vaccineTypeDescription);
        vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);

        Vaccine vaccine = new Vaccine();
        vaccine.setCode(vaccineCode);
        vaccine.setDescription(vaccineDescription);
        vaccine.setVaccineType(vaccineType);

        if (vaccineAlreadyExists) {
            vaccineIoOperationRepository.saveAndFlush(vaccine);
            boolean result = vaccineIoOperation.deleteVaccine(vaccine);
            assertThat(result).isTrue();
            result = vaccineIoOperation.isCodePresent(vaccineCode);
            assertThat(result).isFalse();
        } else {
            boolean result = vaccineIoOperation.isCodePresent(vaccineCode);
            assertThat(result).isFalse();
            result = vaccineIoOperation.deleteVaccine(vaccine);
            assertThat(result).isTrue();
            result = vaccineIoOperation.isCodePresent(vaccineCode);
            assertThat(result).isFalse();
        }
		
    }

    @Test
    public void testIoIsCodePresent() throws Exception {
        class Params {
            String vaccineCode;
            boolean vaccineAlreadyExists;

            Params(String vaccineCode, boolean vaccineAlreadyExists) {
                this.vaccineCode = vaccineCode;
                this.vaccineAlreadyExists = vaccineAlreadyExists;
            }
        }

        List<Params> paramsList = Arrays.asList(
            /* Classe Válida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - Vacina existe no banco de dados
            * Resultado esperado: A função IsCodePresent deve retornar true */
            new Params("0000000000", true),


            /* Classe Válida 2:
            *  - vaccineCode: string com 10 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função IsCodePresent deve retornar false */
            new Params("1111111111", false),

            /* Classe Inválida 1:
            *  - vaccineCode: string com mais de 10 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função IsCodePresent deve retornar false */
            new Params("22222222222", false),

            /* Classe Inválida 2:
            *  - vaccineCode: string vazia
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função IsCodePresent deve retornar false */
            new Params("", false)
        );

        List<Throwable> exceptions = new ArrayList<>();

        for (Params params : paramsList) {
            LOGGER.info("Running test with parameters: " + params.vaccineCode + ", " + params.vaccineAlreadyExists);
            try {
                testIoIsCodePresentWithParams(params.vaccineCode, params.vaccineAlreadyExists);
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

    private void testIoIsCodePresentWithParams(String vaccineCode, boolean vaccineAlreadyExists) throws Exception {
        VaccineType vaccineType = new VaccineType();
        vaccineType.setCode("Z");
        vaccineType.setDescription("Description");
        vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);

        Vaccine vaccine = new Vaccine();
        vaccine.setCode(vaccineCode);
        vaccine.setDescription("Description");
        vaccine.setVaccineType(vaccineType);

        if (vaccineAlreadyExists) {
            vaccineIoOperationRepository.saveAndFlush(vaccine);
            boolean result = vaccineIoOperation.isCodePresent(vaccineCode);
            assertThat(result).isTrue();
        } else {
            boolean result = vaccineIoOperation.isCodePresent(vaccineCode);
            assertThat(result).isFalse();
        }
		
    }

    @Test
    public void testIoFindVaccine() throws Exception {
        class Params {
            String vaccineCode;
            boolean vaccineAlreadyExists;

            Params(String vaccineCode, boolean vaccineAlreadyExists) {
                this.vaccineCode = vaccineCode;
                this.vaccineAlreadyExists = vaccineAlreadyExists;
            }
        }

        List<Params> paramsList = Arrays.asList(
            /* Classe Válida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - Vacina existe no banco de dados
            * Resultado esperado: A função findVaccine deve retornar a vacina */
            new Params("0000000000", true),


            /* Classe Válida 2:
            *  - vaccineCode: string com 10 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função findVaccine deve retornar null */
            new Params("1111111111", false),

            /* Classe Inválida 1:
            *  - vaccineCode: string com mais de 10 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função findVaccine deve retornar null */
            new Params("22222222222", false),

            /* Classe Inválida 2:
            *  - vaccineCode: string vazia
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função findVaccine deve retornar null */
            new Params("", false)
        );

        List<Throwable> exceptions = new ArrayList<>();

        for (Params params : paramsList) {
            LOGGER.info("Running test with parameters: " + params.vaccineCode + ", " + params.vaccineAlreadyExists);
            try {
                testIoFindVaccineWithParams(params.vaccineCode, params.vaccineAlreadyExists);
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

    private void testIoFindVaccineWithParams(String vaccineCode, boolean vaccineAlreadyExists) throws Exception {
        VaccineType vaccineType = new VaccineType();
        vaccineType.setCode("Z");
        vaccineType.setDescription("Description");
        vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);

        Vaccine vaccine = new Vaccine();
        vaccine.setCode(vaccineCode);
        vaccine.setDescription("Description");
        vaccine.setVaccineType(vaccineType);

        if (vaccineAlreadyExists) {
            vaccineIoOperationRepository.saveAndFlush(vaccine);
            Vaccine foundVaccine = vaccineIoOperation.findVaccine(vaccineCode);
            assertThat(foundVaccine).isNotNull();
		    assertThat(foundVaccine.getCode()).isEqualTo(vaccineCode);
        } else {
            Vaccine foundVaccine = vaccineIoOperation.findVaccine(vaccineCode);
            assertThat(foundVaccine).isNull();
        }
		
    }

    @Test
    public void testIoGetVaccine() throws Exception {
        class Params {
            String vaccineCode;
            String vaccineTypeCode;
            boolean vaccineAlreadyExists;

            Params(String vaccineCode, String vaccineTypeCode, boolean vaccineAlreadyExists) {
                this.vaccineCode = vaccineCode;
                this.vaccineTypeCode = vaccineTypeCode;
                this.vaccineAlreadyExists = vaccineAlreadyExists;
            }
        }

        List<Params> paramsList = Arrays.asList(
            /* Classe Válida 1:
            *  - vaccineCode: string com 10 caracteres
            *  - Vacina existe no banco de dados
            * Resultado esperado: A função getVaccine deve retornar a vacina */
            new Params("0000000000", "Z", true),


            /* Classe Válida 2:
            *  - vaccineCode: string com 10 caracteres
            *  - Vacina não existe no banco de dados
            * Resultado esperado: A função getVaccine deve retornar vazio */
            new Params("1111111111", "X", false)
        );

        List<Throwable> exceptions = new ArrayList<>();

        for (Params params : paramsList) {
            LOGGER.info("Running test with parameters: " + params.vaccineCode + ", " + params.vaccineAlreadyExists);
            try {
                testIoGetVaccineWithParams(params.vaccineCode, params.vaccineTypeCode, params.vaccineAlreadyExists);
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

    private void testIoGetVaccineWithParams(String vaccineCode, String vaccineTypeCode, boolean vaccineAlreadyExists) throws Exception {
        VaccineType vaccineType = new VaccineType();
        vaccineType.setCode("Z");
        vaccineType.setDescription("TypeDescription");
        
        Vaccine vaccine = new Vaccine();
        if (vaccineCode == null){
            vaccine.setCode("1234567890");
        }
        vaccine.setCode(vaccineCode);
        vaccine.setDescription("Description");
        vaccine.setVaccineType(vaccineType);

        if (vaccineAlreadyExists) {
            vaccineTypeIoOperationRepository.saveAndFlush(vaccineType);
            vaccineIoOperationRepository.saveAndFlush(vaccine);
            List<Vaccine> vaccines = vaccineIoOperation.getVaccine(vaccineTypeCode);
            assertThat(vaccines.get(vaccines.size() - 1).getDescription()).isEqualTo(vaccine.getDescription());
        } else {
            List<Vaccine> vaccines = vaccineIoOperation.getVaccine(vaccineTypeCode);
            assertThat(vaccines).isEmpty();
        }
		
    }

    private void _checkVaccineIntoDb(String code, String description) throws OHException {
        Vaccine foundVaccine = vaccineIoOperation.findVaccine(code);
        LOGGER.info("Found vaccine: " + foundVaccine.getCode());
        assertThat(foundVaccine.getCode()).isEqualTo(code);
		assertThat(foundVaccine.getDescription()).isEqualTo(description);
    }
}