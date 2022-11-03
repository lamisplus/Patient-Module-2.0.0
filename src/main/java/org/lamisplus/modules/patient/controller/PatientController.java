package org.lamisplus.modules.patient.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lamisplus.modules.patient.domain.dto.PersonDto;
import org.lamisplus.modules.patient.domain.dto.PersonMetaDataDto;
import org.lamisplus.modules.patient.domain.dto.PersonResponseDto;
import org.lamisplus.modules.patient.domain.entity.PatientCheckPostService;
import org.lamisplus.modules.patient.domain.entity.Person;
import org.lamisplus.modules.patient.repository.PatientCheckPostServiceRepository;
import org.lamisplus.modules.patient.service.PersonService;
import org.lamisplus.modules.patient.service.ValidationService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/patient")
public class PatientController {

    private final PersonService personService;

    private final ValidationService validationService;
    private final PatientCheckPostServiceRepository patientCheckPostServiceRepository;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PersonResponseDto> createPatient(@RequestBody PersonDto patient) {
        return ResponseEntity.ok(personService.createPerson(patient));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PersonResponseDto>> getAllPatients() {
        return ResponseEntity.ok(personService.getAllPerson());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<PersonResponseDto> getPatient(@PathVariable("id") Long id) {
        return ResponseEntity.ok(personService.getPersonById(id));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PersonResponseDto> updatePatient(@PathVariable("id") Long id, @RequestBody PersonDto patient) {
        return ResponseEntity.ok(personService.updatePerson(id, patient));
    }

    @DeleteMapping(value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deletePerson(@PathVariable("id") Long id) {
        personService.deletePersonById(id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping(value = "/post-service")
    public ResponseEntity<List<PatientCheckPostService>> getPatientService() {
        return ResponseEntity.ok(patientCheckPostServiceRepository.findAll());
    }

    @GetMapping(value = "/checked-in-by-service/{serviceCode}")
    public ResponseEntity<List<PersonResponseDto>> getCheckedInPatientByService(@PathVariable("serviceCode") String serviceCode) {
        return ResponseEntity.ok(personService.getCheckedInPersonsByServiceCodeAndVisitId(serviceCode));
    }

    @PostMapping("/exist/hospital-number")
    public ResponseEntity<Boolean> hospitalNumberExists(@RequestBody String hospitalNumber) throws InterruptedException, ExecutionException {
        CompletableFuture<Boolean> hospitalNumberExist = validationService.hospitalNumberExist(hospitalNumber);
        return ResponseEntity.ok(hospitalNumberExist.get());
    }



    @GetMapping(value = "/getall-patients-without-biometric")
    public ResponseEntity<List<PersonResponseDto>> getAllPatientWithoutBiomentic(Pageable pageable) {
        return ResponseEntity.ok(personService.getAllPatientWithoutBiomentic(pageable));
    }

    @PostMapping("/exist/nin-number/{nin}")
    public boolean isNinNumberExisting(@PathVariable("nin") String nin) {
        return personService.isNINExisting(nin);
    }

    @GetMapping(value = "get-person-by-nin/{nin}")
    public ResponseEntity<PersonResponseDto> getPatientByNin(@PathVariable("nin") String nin) {
        return ResponseEntity.ok(personService.getPersonByNin(nin));
    }

    @GetMapping(value = "/list-of-checked-in-persons")
    public ArrayList<PersonResponseDto> listOfCheckedinPersons()
    {
        return personService.getAllActiveVisit();
    }


    @GetMapping(value = "/get-all-patient-pageable", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PersonMetaDataDto> getAllPatientPageable(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<PersonResponseDto> list = personService.getAllPersonPageable(pageNo, pageSize);
        int recordSize = personService.getAllPerson().size();
        double totalPage = (double) recordSize/pageSize;
        int totalPage2 = (int) Math.ceil(totalPage);
        PersonMetaDataDto personMetaDataDto = new PersonMetaDataDto();
        personMetaDataDto.setTotalRecords(recordSize);
        personMetaDataDto.setPageSize(pageSize);
        personMetaDataDto.setTotalPages(totalPage2);
        personMetaDataDto.setCurrentPage(pageNo);
        personMetaDataDto.setRecords(list);

        return new ResponseEntity<> (personMetaDataDto, new HttpHeaders(), HttpStatus.OK);
    }

    @GetMapping(value = "/get-duplicate-hospital_numbers")
    public ResponseEntity<List<PersonResponseDto>> getDuplicateHospitalNumbers() {
        return ResponseEntity.ok(personService.getDuplicateHospitalNumbers());
    }
}
