package org.lamisplus.modules.patient.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.lamisplus.modules.base.controller.apierror.EntityNotFoundException;
import org.lamisplus.modules.base.controller.apierror.RecordExistException;
import org.lamisplus.modules.patient.domain.dto.*;
import org.lamisplus.modules.patient.domain.entity.Encounter;
import org.lamisplus.modules.patient.domain.entity.Person;
import org.lamisplus.modules.patient.domain.entity.Visit;
import org.lamisplus.modules.patient.repository.EncounterRepository;
import org.lamisplus.modules.patient.repository.PatientCheckPostServiceRepository;
import org.lamisplus.modules.patient.repository.PersonRepository;
import org.lamisplus.modules.patient.repository.VisitRepository;
import org.lamisplus.modules.patient.utility.LocalDateConverter;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.persistence.Convert;
import javax.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitService {
    private final PersonRepository personRepository;
    private final VisitRepository visitRepository;

    private final EncounterRepository encounterRepository;

    private final PatientCheckPostServiceRepository patientCheckPostServiceRepository;


    public VisitDto createVisit(VisitRequestDto visitDto) {


        String checkInDate = visitDto.getVisitDto().getCheckInDate();
        Person person = personRepository
                .findById(visitDto.getVisitDto().getPersonId())
                .orElseThrow(() -> new EntityNotFoundException(VisitService.class, "errorMessage", "No person found with id " + visitDto.getVisitDto().getPersonId()));


        Optional<Visit> currentVisit = visitRepository.findVisitByPersonAndVisitStartDateNotNullAndVisitEndDateIsNull(person);
        if (currentVisit.isPresent())
            throw new RecordExistException(VisitService.class, "errorMessage", "Visit Already exist for this patient " + person.getId());
        Visit visit = convertDtoToEntityVisit(visitDto);

        visitDto.getServiceIds().stream()
                .map(id -> patientCheckPostServiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(Visit.class, "errorMessage", "No service found with Id " + id)))
                .forEach(patientCheckPostService -> createEncounter(person, visit, patientCheckPostService.getModuleServiceCode()));

        visit.setUuid(UUID.randomUUID().toString());
        visit.setArchived(0);
        if (checkInDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime visitStartDateTime = LocalDateTime.parse(checkInDate, formatter);
            visit.setVisitStartDate(visitStartDateTime);
        } else {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formatDateTime = now.format(formatter);
            LocalDateTime visitStartDateTime = LocalDateTime.parse(formatDateTime, formatter);
            visit.setVisitStartDate(visitStartDateTime);
        }
        return convertEntityToDto(visitRepository.save(visit));
    }

    public VisitDto updateVisit(Long id, VisitDto visitDto) {
        Visit existVisit = getExistVisit(id);
        Visit visit = convertDtoToEntity(visitDto);
        visit.setId(existVisit.getId());
        visit.setArchived(0);
        return convertEntityToDto(visitRepository.save(visit));

    }

    public void checkOutVisitById(Long visitId) {
        Visit visit = getExistVisit(visitId);
        List<Encounter> encounters = encounterRepository.getEncounterByVisit(visit);
        encounters.forEach(this::checkoutFromAllService);
        visit.setVisitEndDate(LocalDateTime.now());
        visitRepository.save(visit);
    }

    private void checkoutFromAllService(Encounter encounter) {
        if (encounter.getStatus().equals("PENDING")) {
            encounter.setStatus("COMPLETED");
        }
        encounterRepository.save(encounter);
    }

    public VisitDto getVisitById(Long id) {
        return convertEntityToDto(getExistVisit(id));
    }

    public List<VisitDto> getAllVisit() {
        return visitRepository
                .findAllByArchived(0)
                .stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }

    public void archivedVisit(Long id) {
        Visit existVisit = getExistVisit(id);
        existVisit.setArchived(1);
        visitRepository.save(existVisit);
    }

    public VisitDto checkInPerson(CheckInDto checkInDto) {
        Long personId = checkInDto.getVisitDto().getVisitDto().getPersonId();
        Person person = personRepository
                .findById(personId)
                .orElseThrow(() -> new EntityNotFoundException(Visit.class, "errorMessage", "No person found with id " + personId));
        VisitDto visitDto = createVisit(checkInDto.getVisitDto());
        Visit visit = getExistVisit(visitDto.getId());
        checkInDto.getServiceIds()
                .stream()
                .map(id -> patientCheckPostServiceRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(Visit.class, "errorMessage", "No service found with Id " + id)))
                .forEach(patientCheckPostService -> createEncounter(person, visit, patientCheckPostService.getModuleServiceCode()));
        return visitDto;
    }

    private void createEncounter(Person person, Visit visit, String serviceCode) {
        Encounter encounter = getEncounter(person, visit);
        encounter.setServiceCode(serviceCode);
        encounter.setFacilityId(visit.getFacilityId());
        encounter.setEncounterDate(LocalDateTime.now());
        encounterRepository.save(encounter);
    }

    private Encounter getEncounter(Person person, Visit visit) {
        return Encounter.builder()
                .person(person)
                .archived(0)
                .visit(visit)
                .encounterDate(visit.getVisitStartDate())
                .uuid(UUID.randomUUID().toString())
                .status("PENDING")
                .build();
    }

    public List<VisitDetailDto> getVisitWithEncounterDetails(Long personId) {
        Optional<Person> person = personRepository.findById(personId);
        return person.map(value -> encounterRepository.getEncounterByPersonAndArchived(value, 0)
                .stream()
                .map(encounter -> getVisitDetailDto(personId, encounter)).collect(Collectors.toList())).orElseGet(ArrayList::new);

    }

    private VisitDetailDto getVisitDetailDto(Long personId, Encounter encounter) {
        List<Encounter> encounters = this.encounterRepository.getEncounterByVisit(encounter.getVisit());
        List<EncounterResponseDto> encounterResponseList = new ArrayList<>();
        encounters.forEach(encounter1 -> {
            EncounterResponseDto encounterResponseDto = new EncounterResponseDto();
            encounterResponseDto.setFacilityId(encounter1.getFacilityId());
            encounterResponseDto.setId(encounter1.getId());
            encounterResponseDto.setEncounterDate(encounter1.getEncounterDate().toLocalDate());
            encounterResponseDto.setPersonId(encounter1.getPerson().getId());
            encounterResponseDto.setUuid(encounter1.getUuid());
            encounterResponseDto.setVisitId(encounter1.getVisit().getId());
            encounterResponseDto.setServiceCode(encounter1.getServiceCode());
            encounterResponseDto.setStatus(encounter1.getStatus());
            encounterResponseList.add(encounterResponseDto);

        });
        return VisitDetailDto.builder()
                .status(encounter.getStatus())
                .id(encounter.getVisit().getId())
                .personId(personId)
                .checkInDate(encounter.getVisit().getVisitStartDate())
                .checkOutDate(encounter.getVisit().getVisitEndDate())
                .encounterId(encounter.getId())
                .service(encounter.getServiceCode())
                .encounters(encounterResponseList)
                .build();
    }

    private Visit getExistVisit(Long id) {
        return visitRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VisitService.class, "errorMessage", "No visit was found with given Id " + id));
    }


    private Visit convertDtoToEntity(VisitDto visitDto) {
        Person person = personRepository
                .findById(visitDto.getPersonId())
                .orElseThrow(() -> new EntityNotFoundException(VisitService.class, "errorMessage", "No patient found with id " + visitDto.getPersonId()));
        Visit visit = new Visit();
        BeanUtils.copyProperties(visitDto, visit);
        visit.setVisitStartDate(LocalDateTime.now());
        log.info("facilityId {}", person.getFacilityId());
        visit.setFacilityId(person.getFacilityId());
        visit.setPerson(person);
        return visit;
    }

    private Visit convertDtoToEntityVisit(VisitRequestDto visitDto) {
        Person person = personRepository
                .findById(visitDto.getVisitDto().getPersonId())
                .orElseThrow(() -> new EntityNotFoundException(VisitService.class, "errorMessage", "No patient found with id " + visitDto.getVisitDto().getPersonId()));
        Visit visit = new Visit();
        BeanUtils.copyProperties(visitDto, visit);
        visit.setVisitStartDate(LocalDateTime.now());
        log.info("facilityId {}", person.getFacilityId());
        visit.setFacilityId(person.getFacilityId());
        visit.setPerson(person);
        return visit;
    }

    private VisitDto convertEntityToDto(Visit visit) {
        VisitDto visitDto = new VisitDto();
        BeanUtils.copyProperties(visit, visitDto);
        visitDto.setPersonId(visit.getPerson().getId());
        visitDto.setFacilityId(visit.getFacilityId());
        visitDto.setId(visit.getId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String checkInDate = visit.getVisitStartDate().format(formatter);
        String checkInOut = visit.getVisitEndDate().format(formatter);
        visitDto.setCheckInDate(checkInDate);
        visitDto.setCheckOutDate(checkInOut);
        List<Encounter> encounters = this.encounterRepository.getEncounterByVisit(visit);
        List<EncounterResponseDto> encounterResponseList = new ArrayList<>();
        encounters.forEach(encounter -> {
            EncounterResponseDto encounterResponseDto = new EncounterResponseDto();
            encounterResponseDto.setFacilityId(encounter.getFacilityId());
            encounterResponseDto.setId(encounter.getId());
            encounterResponseDto.setEncounterDate(encounter.getEncounterDate().toLocalDate());
            encounterResponseDto.setPersonId(encounter.getPerson().getId());
            encounterResponseDto.setUuid(encounter.getUuid());
            encounterResponseDto.setVisitId(encounter.getVisit().getId());
            encounterResponseDto.setServiceCode(encounter.getServiceCode());
            encounterResponseDto.setStatus(encounter.getStatus());
            encounterResponseList.add(encounterResponseDto);

        });
        visitDto.setEncounters(encounterResponseList);

        return visitDto;
    }

}
