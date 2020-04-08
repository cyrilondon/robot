package com.game.domain.model.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.game.domain.application.GameContext;
import com.game.domain.application.GameService;
import com.game.domain.application.GameServiceImpl;
import com.game.domain.application.command.InitializeRoverCommand;
import com.game.domain.application.command.MoveRoverCommand;
import com.game.domain.model.entity.Orientation;
import com.game.domain.model.entity.Plateau;
import com.game.domain.model.entity.Rover;
import com.game.domain.model.entity.dimensions.RelativisticTwoDimensions;
import com.game.domain.model.entity.dimensions.TwoDimensionalCoordinates;
import com.game.domain.model.entity.dimensions.TwoDimensions;
import com.game.domain.model.exception.GameExceptionLabels;
import com.game.domain.model.exception.IllegalArgumentGameException;
import com.game.domain.model.exception.PlateauLocationAlreadySetException;

public class GameServiceImplTest {

	private static final int WIDTH = 5;

	private static final int HEIGHT = 5;

	private static final int X = 3;

	private static final int Y = 4;

	private GameContext gameContext = GameContext.getInstance();

	private GameService gameService = new GameServiceImpl();

	public List<Rover> roversList = new ArrayList<Rover>();

	public Plateau plateau;

	@BeforeTest
	public void setup() {
		ServiceLocator mockServiceLocator = new ServiceLocator();
		mockServiceLocator.loadService(ServiceLocator.ROVER_SERVICE, new MockRoverServiceImpl());
		mockServiceLocator.loadService(ServiceLocator.PLATEAU_SERVICE, new MockPlateauServiceImpl());
		ServiceLocator.load(mockServiceLocator);
	}

	@BeforeMethod
	public void resetGame() {
		gameContext.reset();
		roversList.clear();
	}

	@Test
	public void testInitializePlateau() {
		gameService.initializePlateau(UUID.randomUUID(), new TwoDimensionalCoordinates(WIDTH, HEIGHT));
		assertThat(plateau.getWidth()).isEqualTo(WIDTH);
		assertThat(plateau.getHeight()).isEqualTo(HEIGHT);
		assertThat(gameContext.isInitialized());
		assertThat(gameContext.getPlateau()).isEqualTo(plateau);
	}

	@Test
	public void testInitializeRelativisticPlateau() {
		gameService.initializeRelativisticPlateau(UUID.randomUUID(), 12, new TwoDimensionalCoordinates(WIDTH, HEIGHT));
		assertThat(plateau.getWidth()).isEqualTo(WIDTH - 1);
		assertThat(plateau.getHeight()).isEqualTo(HEIGHT - 1);
		assertThat(gameContext.isInitialized());
		assertThat(gameContext.getPlateau()).isEqualTo(plateau);
	}

	@Test
	public void testInitializeRover() {
		UUID uuid = UUID.randomUUID();
		gameContext.addPlateau(getPlateau(uuid));
		TwoDimensionalCoordinates coordinates = new TwoDimensionalCoordinates(X, Y);
		InitializeRoverCommand initializeCommand = new InitializeRoverCommand.Builder().withPlateauUuid(uuid)
				.withAbscissa(coordinates.getAbscissa()).withOrdinate(coordinates.getOrdinate()).withOrientation('S')
				.build();
		gameService.execute(initializeCommand);
		assertThat(roversList.contains(
				new Rover(uuid, GameContext.ROVER_NAME_PREFIX + gameContext.getCounter(), coordinates, Orientation.SOUTH)))
						.isTrue();
		assertThat(gameContext.getPlateauService().isLocationBusy(uuid, coordinates)).isTrue();
		TwoDimensionalCoordinates otherCoordinates = new TwoDimensionalCoordinates(X + 1, Y + 1);
		InitializeRoverCommand otherInitializeCommand = new InitializeRoverCommand.Builder().withPlateauUuid(uuid)
				.withAbscissa(otherCoordinates.getAbscissa()).withOrdinate(otherCoordinates.getOrdinate())
				.withOrientation('E').build();
		gameService.execute(otherInitializeCommand);
		assertThat(roversList.contains(new Rover(uuid, GameContext.ROVER_NAME_PREFIX + gameContext.getCounter(),
				otherCoordinates, Orientation.EAST))).isTrue();
		assertThat(gameContext.getPlateauService().isLocationBusy(uuid, otherCoordinates)).isTrue();
	}

	/**
	 * Expected error message: "[ERR-000] Missing Plateau configuration - It is not
	 * allowed to add a Rover. Please initialize the Plateau first."
	 */
	@Test
	public void testInitializeRoverWithoutPlateau() {
		InitializeRoverCommand initializeCommand = new InitializeRoverCommand.Builder().withAbscissa(X).withOrdinate(Y)
				.withOrientation('S').build();
		Throwable thrown = catchThrowable(() -> gameService.execute(initializeCommand));
		assertThat(thrown).isInstanceOf(IllegalArgumentGameException.class)
				.hasMessage(String.format(GameExceptionLabels.ERROR_CODE_AND_MESSAGE_PATTERN,
						GameExceptionLabels.ILLEGAL_ARGUMENT_CODE,
						String.format(GameExceptionLabels.ERROR_MESSAGE_SEPARATION_PATTERN,
								GameExceptionLabels.MISSING_PLATEAU_CONFIGURATION,
								GameExceptionLabels.ADDING_ROVER_NOT_ALLOWED)));
	}

	@Test
	public void testMoveRoverWithOrientation() {
		String roverName = GameContext.ROVER_NAME_PREFIX + 3;
		gameService.execute(new MoveRoverCommand(UUID.randomUUID(),roverName, 1));
		assertThat(roversList).contains(new Rover(UUID.randomUUID(), roverName, new TwoDimensionalCoordinates(2, 3), Orientation.WEST));

	}

	/**
	 * Here we test that no exception is caught in GameServiceImpl method
	 */
	@Test
	public void testMoveRoverWithOrientationOutOfTheBoard() {
		String roverName = GameContext.ROVER_NAME_PREFIX + 5;
		Throwable thrown = catchThrowable(() -> gameService.execute(new MoveRoverCommand(UUID.randomUUID(), roverName, 1)));
		assertThat(thrown).isInstanceOf(PlateauLocationAlreadySetException.class)
				.hasMessage(String.format(GameExceptionLabels.ERROR_CODE_AND_MESSAGE_PATTERN,
						GameExceptionLabels.PLATEAU_LOCATION_ERROR_CODE, "Error"));

	}

	private Plateau getPlateau(UUID uuid) {
		return new Plateau(uuid, new TwoDimensions(new TwoDimensionalCoordinates(WIDTH, HEIGHT)));
	}

	/**
	 * Simple MockClass for the RoverServiceImpl
	 *
	 */
	private class MockRoverServiceImpl implements RoverService {

		@Override
		public void initializeRover(UUID plateauUuid, String roverName, TwoDimensionalCoordinates coordinates, Orientation orientation) {
			GameServiceImplTest.this.roversList.add(new Rover(plateauUuid, roverName, coordinates, orientation));
		}

		@Override
		public void faceToOrientation(String roverName, Orientation orientation) {

		}

		@Override
		public void moveRoverNumberOfTimes(String roverName, int numberOfTimes) {
			GameServiceImplTest.this.roversList
					.add(new Rover(UUID.randomUUID(), roverName, new TwoDimensionalCoordinates(2, 3), Orientation.WEST));
			if (roverName.equals(GameContext.ROVER_NAME_PREFIX + 5))
				throw new PlateauLocationAlreadySetException("Error");
		}

		@Override
		public void updateRover(Rover rover) {
		}

		@Override
		public Rover getRover(String roverName) {
			return null;
		}

	}

	/**
	 * Simple MockClass for the PlateauServiceImpl
	 *
	 */
	private class MockPlateauServiceImpl implements PlateauService {

		Map<TwoDimensionalCoordinates, Boolean> mapLocations = new HashMap<>();

		@Override
		public Plateau initializePlateau(UUID uuid, TwoDimensionalCoordinates coordinates) {
			GameServiceImplTest.this.plateau = new Plateau(uuid, new TwoDimensions(
					new TwoDimensionalCoordinates(coordinates.getAbscissa(), coordinates.getOrdinate())));
			return GameServiceImplTest.this.plateau;
		}

		@Override
		public Plateau initializeRelativisticPlateau(UUID uuid, int speed, TwoDimensionalCoordinates coordinates) {
			GameServiceImplTest.this.plateau = new Plateau(uuid, new RelativisticTwoDimensions(speed, new TwoDimensions(
					new TwoDimensionalCoordinates(coordinates.getAbscissa(), coordinates.getOrdinate()))));
			return GameServiceImplTest.this.plateau;
		}

		@Override
		public void setLocationBusy(UUID uuid, TwoDimensionalCoordinates coordinates) {
			mapLocations.put(coordinates, Boolean.TRUE);
		}

		@Override
		public boolean isLocationBusy(UUID uuid, TwoDimensionalCoordinates coordinates) {
			return mapLocations.get(coordinates);
		}

		@Override
		public void setLocationFree(UUID uuid, TwoDimensionalCoordinates coordinates) {
			mapLocations.put(coordinates, Boolean.FALSE);
			
		}

	}

}
