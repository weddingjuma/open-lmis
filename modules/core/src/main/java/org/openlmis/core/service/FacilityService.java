/*
 * Copyright © 2013 VillageReach.  All Rights Reserved.  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.openlmis.core.service;


import lombok.NoArgsConstructor;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.joda.time.DateTime;
import org.openlmis.core.domain.*;
import org.openlmis.core.dto.FacilityFeedDTO;
import org.openlmis.core.repository.FacilityRepository;
import org.openlmis.core.repository.GeographicZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URISyntaxException;
import java.util.*;

@Service
@NoArgsConstructor
public class FacilityService {

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private ProgramSupportedService programSupportedService;

  @Autowired
  private RequisitionGroupService requisitionGroupService;

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  @Autowired
  private SupervisoryNodeService supervisoryNodeService;

  @Autowired
  private EventService eventService;

  @Autowired
  private FacilityProgramProductService facilityProgramProductService;


  @Transactional
  public void insert(Facility facility) {
    save(facility);
    programSupportedService.addSupportedProgramsFor(facility);
  }

  @Transactional
  public void update(Facility facility) {
    save(facility);
    programSupportedService.updateSupportedPrograms(facility, programSupportedService.getAllByFacilityId(facility.getId()));
  }

  public List<Facility> getAll() {
    return facilityRepository.getAll();
  }

  public List<FacilityType> getAllTypes() {
    return facilityRepository.getAllTypes();
  }

  public List<FacilityOperator> getAllOperators() {
    return facilityRepository.getAllOperators();
  }

  public List<GeographicZone> getAllZones() {
    return geographicZoneRepository.getAllGeographicZones();
  }

  public Facility getHomeFacility(Long userId) {
    return facilityRepository.getHomeFacility(userId);
  }

  public Facility getById(Long id) {
    Facility facility = facilityRepository.getById(id);
    facility.setSupportedPrograms(programSupportedService.getAllByFacilityId(id));
    return facility;
  }

  public Facility updateDataReportableAndActiveFor(Facility facility) {
    Facility updatedFacility = facilityRepository.updateDataReportableAndActiveFor(facility);
    updatedFacility.setSupportedPrograms(programSupportedService.getAllByFacilityId(facility.getId()));

    getFacilityAndNotify(facility);

    return updatedFacility;
  }

  private void getFacilityAndNotify(Facility facility) {
    facility = facilityRepository.getById(facility.getId());

    notifyFacilityFeed(facility);
  }

  public List<Facility> getUserSupervisedFacilities(Long userId, Long programId, Right... rights) {
    List<SupervisoryNode> supervisoryNodes = supervisoryNodeService.getAllSupervisoryNodesInHierarchyBy(userId, programId, rights);
    List<RequisitionGroup> requisitionGroups = requisitionGroupService.getRequisitionGroupsBy(supervisoryNodes);
    return facilityRepository.getFacilitiesBy(programId, requisitionGroups);
  }

  public List<Facility> searchFacilitiesByCodeOrName(String searchParam) {
    return facilityRepository.searchFacilitiesByCodeOrName(searchParam);
  }

  public void save(Facility facility) {
    for (ProgramSupported programSupported : facility.getSupportedPrograms()) {
      programSupported.isValid();
    }
    facilityRepository.save(facility);
    getFacilityAndNotify(facility);
  }

  private void notifyFacilityFeed(Facility facility) {
    try {
      FacilityFeedDTO facilityFeedDTO = new FacilityFeedDTO(facility);
      eventService.notify(new Event(UUID.randomUUID().toString(), "Facility", DateTime.now(), "", facilityFeedDTO.getSerializedContents(), "facility"));
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public List<Facility> getForUserAndRights(Long userId, Right... rights) {
    List<SupervisoryNode> supervisoryNodesInHierarchy = supervisoryNodeService.getAllSupervisoryNodesInHierarchyBy(userId, rights);
    List<RequisitionGroup> requisitionGroups = requisitionGroupService.getRequisitionGroupsBy(supervisoryNodesInHierarchy);
    final Set<Facility> userFacilities = new HashSet<>(facilityRepository.getAllInRequisitionGroups(requisitionGroups));
    final Facility homeFacility = facilityRepository.getHomeFacilityForRights(userId, rights);

    if (homeFacility != null) userFacilities.add(homeFacility);

    return new ArrayList<>(userFacilities);

  }

  public FacilityType getFacilityTypeByCode(FacilityType facilityType) {
    return facilityRepository.getFacilityTypeByCode(facilityType);
  }

  public Facility getByCode(Facility facility) {
    return facilityRepository.getByCode(facility);
  }

  public List<Facility> getAllForDeliveryZoneAndProgram(Long deliveryZoneId, Long programId) {
    List<Facility> facilities = facilityRepository.getAllInDeliveryZoneFor(deliveryZoneId, programId);
    for(Facility facility : facilities) {
      facility.getSupportedPrograms().add(programSupportedService.getByFacilityIdAndProgramId(facility.getId(), programId));
    }
    return facilities;
  }
}
