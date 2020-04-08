package com.game.domain.application;

import java.util.UUID;

import com.game.domain.application.command.InitializeRoverCommand;
import com.game.domain.application.command.MoveRoverCommand;
import com.game.domain.model.DomainEventPublisher;
import com.game.domain.model.DomainEventSubscriber;
import com.game.domain.model.RoverMovedEvent;
import com.game.domain.model.entity.Orientation;
import com.game.domain.model.entity.Plateau;
import com.game.domain.model.entity.Rover;
import com.game.domain.model.entity.dimensions.TwoDimensionalCoordinates;
import com.game.domain.model.exception.GameExceptionLabels;
import com.game.domain.model.exception.IllegalArgumentGameException;
import com.game.domain.model.service.PlateauServiceImpl;


/**
 * Application service which acts as a facade to the application
 * <ol>
 *  <li>Delegates the execution of the process to the two Domain services
 *  {@link RoverServiceImpl}, {@link PlateauServiceImpl} and the Application
 *  State {@link GameContext}</li>
 *  <li>Converts Command objects from outside world to Domain Services calls
 *  (All the write commands should have return type = void)</li>
 *  <li>Register Domain Events subscribers</li>
 * </ol>
 */
public class GameServiceImpl implements GameService {

	GameContext gameContext = GameContext.getInstance();

	public void initializePlateau(UUID uuid, TwoDimensionalCoordinates coordinates) {
		Plateau plateau = gameContext.getPlateauService().initializePlateau(uuid, coordinates);
		addPlateauToContext(plateau);
	}

	public void initializeRelativisticPlateau(UUID uuid, int speed, TwoDimensionalCoordinates coordinates) {
		Plateau plateau = gameContext.getPlateauService().initializeRelativisticPlateau(uuid, speed, coordinates);
		addPlateauToContext(plateau);
	}

	public void execute(InitializeRoverCommand command) {
		if (!gameContext.isInitialized())
			throw new IllegalArgumentGameException(String.format(GameExceptionLabels.ERROR_MESSAGE_SEPARATION_PATTERN,
					GameExceptionLabels.MISSING_PLATEAU_CONFIGURATION,
					GameExceptionLabels.ADDING_ROVER_NOT_ALLOWED));
		int robotNumber = gameContext.getCounter().addAndGet(1);
		gameContext.getRoverService().initializeRover(command.getPlateauUuid(), GameContext.ROVER_NAME_PREFIX + robotNumber, new TwoDimensionalCoordinates(command.getAbscissa(), command.getOrdinate()),
				Orientation.get(String.valueOf(command.getOrientation())));
		gameContext.getPlateauService().setLocationBusy(command.getPlateauUuid(), new TwoDimensionalCoordinates(command.getAbscissa(), command.getOrdinate()));
	}
	
	@SuppressWarnings("unchecked")
	public void execute(MoveRoverCommand command) {
		@SuppressWarnings("rawtypes")
		DomainEventSubscriber subscriber = new DomainEventSubscriber<RoverMovedEvent>() {

			@Override
			public void handleEvent(RoverMovedEvent event) {
				// 1. update persistent Rover with last position
				updateRoverWithLastPosition(event);
				// 2. mark old rover position as free
				gameContext.getPlateauService().setLocationFree(event.getPlateauUuid(), event.getPreviousPosition());
				// 3. mark new rover position as set/busy
				gameContext.getPlateauService().setLocationBusy(event.getPlateauUuid(), event.getCurrentPosition());
				// 4s. store the event
			}

			@Override
			public Class<RoverMovedEvent> subscribedToEventType() {
				return RoverMovedEvent.class;
			}
			
			private void updateRoverWithLastPosition(RoverMovedEvent event) {
				Rover rover = gameContext.getRoverService().getRover(event.getRoverName());
				rover.setPosition(event.getCurrentPosition());
				gameContext.getRoverService().updateRover(rover);
			}
		};
		DomainEventPublisher.instance().subscribe(subscriber);
		gameContext.getRoverService().moveRoverNumberOfTimes(command.getRoverName(), command.getNumberOfMoves());
	}

	/**
	 * Once initialized, we want to keep track of the Plateau as in-memory singleton
	 * instance during the game lifetime i.e no need to go back to the Plateau
	 * repository each time it is needed (this in contrary to what happens for the
	 * rover objects which are fetched each time from the Rover Repository)
	 * 
	 * @param plateau
	 */
	private void addPlateauToContext(Plateau plateau) {
		gameContext.addPlateau(plateau);
	}

}
