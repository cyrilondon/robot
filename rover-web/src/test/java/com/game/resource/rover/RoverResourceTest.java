package com.game.resource.rover;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.game.BaseUnitTest;
import com.game.Main;

public class RoverResourceTest extends BaseUnitTest {

	/**
	 * Test to see that the message "Got it!" is sent in the response.
	 */
	@Test
	public void testGetIt() {
		String responseMsg = target.path("v1/rover").request().get(String.class);
		assertEquals("Got a Rover Resource!", responseMsg);
	}

	@Test
	public void testInitializeRoverAndGet() {

		// Given
		// initialize first a Plateau (needed as we are only in memory for now)
		String plateauUUID = "13567a5e-a21c-495e-80a3-d12adaf8585c";
		initializePlateau(plateauUUID);

		String roverName = "ROVER_TEST";
		int abscissa = 2;
		int ordinate = 3;
		String orientation = "N";

		String entity = String.format(
				"{\"plateauUuid\": \"%s\", \"name\": \"%s\", \"abscissa\": %d, \"ordinate\": %d, \"orientation\": \"%s\"}",
				plateauUUID, roverName, abscissa, ordinate, orientation);

		// When
		Response response = target.path("v1/rover/initialize").request()
				.post(Entity.entity(entity, MediaType.APPLICATION_JSON));

		// Then
		// check the Response status = 201 for newly created Resource
		assertEquals(201, response.getStatus());
		// check the Location URI response header
		//http://localhost:8080/game/v1/rover/ROVER_TEST/13567a5e-a21c-495e-80a3-d12adaf8585c
		assertEquals(response.getHeaderString(HttpHeaders.LOCATION),
				new StringBuilder(Main.BASE_URI).append("v1/rover/").append(roverName).append("/").append(plateauUUID).toString());

		// rest call to get the Plateau with UUID = plateauUUID
		String getResponse = target.path(String.format("v1/rover/%s/%s", roverName, plateauUUID)).request()
				.get(String.class);
		String expectedResponse = "{\"abscissa\":2,\"name\":\"ROVER_TEST\",\"ordinate\":3,\"orientation\":\"N\",\"plateauUuid\":\"13567a5e-a21c-495e-80a3-d12adaf8585c\"}";

		assertEquals(expectedResponse, getResponse);

	}
}
