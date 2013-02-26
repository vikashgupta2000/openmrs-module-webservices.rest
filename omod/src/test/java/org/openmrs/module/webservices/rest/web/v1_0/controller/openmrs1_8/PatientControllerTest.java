/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller.openmrs1_8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.test.Util;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseCrudControllerTest;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8.ResourceTestConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Tests CRUD operations for {@link Patient}s via web service calls
 */
public class PatientControllerTest extends BaseCrudControllerTest {
	
	private PatientService service;
	
	@Override
	public String getURI() {
		return "patient";
	}
	
	@Override
	public String getUuid() {
		return ResourceTestConstants.PATIENT_UUID;
	}
	
	@Override
	public long getAllCount() {
		return 0;
	}
	
	@Before
	public void before() {
		this.service = Context.getPatientService();
	}
	
	@Test(expected = ResourceDoesNotSupportOperationException.class)
	public void shouldGetAll() throws Exception {
		super.shouldGetAll();
	}
	
	@Test
	public void shouldGetAPatientByUuid() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		SimpleObject result = deserialize(handle(req));
		
		Patient patient = service.getPatientByUuid(getUuid());
		assertEquals(patient.getUuid(), PropertyUtils.getProperty(result, "uuid"));
		assertNotNull(PropertyUtils.getProperty(result, "identifiers"));
		assertNotNull(PropertyUtils.getProperty(result, "person"));
		assertNull(PropertyUtils.getProperty(result, "auditInfo"));
	}
	
	@Test
	public void shouldReturnTheAuditInfoForTheFullRepresentation() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI() + "/" + getUuid());
		req.addParameter(RestConstants.REQUEST_PROPERTY_FOR_REPRESENTATION, RestConstants.REPRESENTATION_FULL);
		SimpleObject result = deserialize(handle(req));
		
		assertNotNull(PropertyUtils.getProperty(result, "auditInfo"));
	}
	
	@Test
	public void shouldCreateAPatient() throws Exception {
		long originalCount = service.getAllPatients().size();
		String json = "{ \"person\": \"ba1b19c2-3ed6-4f63-b8c0-f762dc8d7562\", "
		        + "\"identifiers\": [{ \"identifier\":\"abc123ez\", "
		        + "\"identifierType\":\"2f470aa8-1d73-43b7-81b5-01f0c0dfa53c\", "
		        + "\"location\":\"9356400c-a5a2-4532-8f2b-2361b3446eb8\", " + "\"preferred\": true }] }";
		
		SimpleObject newPatient = deserialize(handle(newPostRequest(getURI(), json)));
		
		assertNotNull(PropertyUtils.getProperty(newPatient, "uuid"));
		assertEquals(originalCount + 1, service.getAllPatients().size());
	}
	
	@Test(expected = ConversionException.class)
	public void shouldNotSupportEditingAPatient() throws Exception {
		final String newPersonUuid = "a7e04421-525f-442f-8138-05b619d16def";
		assertFalse(newPersonUuid.equals(getUuid()));
		SimpleObject patient = new SimpleObject();
		patient.add("person", newPersonUuid);
		
		String json = new ObjectMapper().writeValueAsString(patient);
		
		MockHttpServletRequest req = request(RequestMethod.POST, getURI() + "/" + getUuid());
		req.setContent(json.getBytes());
		handle(req);
	}
	
	@Test
	public void shouldVoidAPatient() throws Exception {
		Patient patient = service.getPatientByUuid(getUuid());
		final String reason = "some random reason";
		assertEquals(false, patient.isVoided());
		MockHttpServletRequest req = newDeleteRequest(getURI() + "/" + getUuid(), new Parameter("!purge", ""),
		    new Parameter("reason", reason));
		handle(req);
		patient = service.getPatientByUuid(getUuid());
		assertTrue(patient.isVoided());
		assertEquals(reason, patient.getVoidReason());
	}
	
	@Test
	public void shouldPurgeAPatient() throws Exception {
		final String uuid = "86526ed6-3c11-11de-a0ba-001e378eb67e";
		assertNotNull(service.getPatientByUuid(uuid));
		MockHttpServletRequest req = newDeleteRequest(getURI() + "/" + uuid, new Parameter("purge", ""));
		handle(req);
		assertNull(service.getPatientByUuid(uuid));
	}
	
	@Test
	public void shouldSearchAndReturnAListOfPatientsMatchingTheQueryString() throws Exception {
		MockHttpServletRequest req = request(RequestMethod.GET, getURI());
		req.addParameter("q", "Horatio");
		SimpleObject result = deserialize(handle(req));
		assertEquals(1, Util.getResultsSize(result));
		assertEquals(getUuid(), PropertyUtils.getProperty(Util.getResultsList(result).get(0), "uuid"));
	}
}