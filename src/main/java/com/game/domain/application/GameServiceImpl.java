package com.game.domain.application;

import java.util.List;

import com.game.domain.application.command.ApplicationCommand;
import com.game.domain.application.command.plateau.PlateauInitializeCommand;
import com.game.domain.application.command.rover.RoverInitializeCommand;
import com.game.domain.application.command.rover.RoverMoveCommand;
import com.game.domain.application.command.rover.RoverTurnCommand;
import com.game.domain.model.entity.Orientation;
import com.game.domain.model.entity.Plateau;
import com.game.domain.model.entity.RoverIdentifier;
import com.game.domain.model.entity.dimensions.TwoDimensionalCoordinates;
import com.game.domain.model.event.DomainEventPublisher;
import com.game.domain.model.event.subscriber.plateau.PlateauSwitchedLocationEventSubscriber;
import com.game.domain.model.event.subscriber.rover.RoverMovedEventSubscriber;
import com.game.domain.model.event.subscriber.rover.RoverMovedWithExceptionEventSubscriber;
import com.game.domain.model.event.subscriber.rover.RoverTurnedEventSubscriber;
import com.game.domain.model.exception.GameExceptionLabels;
import com.game.domain.model.exception.IllegalArgumentGameException;
import com.game.domain.model.exception.PlateauNotFoundException;
import com.game.domain.model.service.PlateauServiceImpl;
import com.game.domain.model.service.RoverServiceImpl;

/**
 * Application service which acts as a facade to the application
 * <ol>
 * <li>Exposes only one public method to the rest of the world {@link #execute(List)}</li>
 * <li>Delegates the execution of the process to the two Domain services
 * {@link RoverServiceImpl}, {@link PlateauServiceImpl} and the Application
 * State {@link GameContext}</li>
 * <li>Converts Command objects from outside world to Domain Services calls (All
 * the write commands should have return type = void)</li>
 * <li>Register Domain Events subscribers</li>
 * </ol>
 */
class GameServiceImpl implements GameService {

	@Override
	public void execute(List<ApplicationCommand> commands) {
		CommandVisitor commandVisitor = new CommandVisitor();
		commands.forEach(command -> command.acceptVisitor(commandVisitor));
	}

	void execute(PlateauInitializeCommand command) {
		GameContext gameContext = GameContext.getInstance();
		Plateau plateau = null;

		// if speed not very important initialize a classical Plateau otherwise a
		// relativistic Plateau
		if (command.getObserverSpeed() < GameContext.MINIMAL_RELATIVISTIC_SPEED) {
			plateau = gameContext.getPlateauService().initializePlateau(command.getPlateauUuid(),
					new TwoDimensionalCoordinates(command.getAbscissa(), command.getOrdinate()));
		} else {
			plateau = gameContext.getPlateauService().initializeRelativisticPlateau(command.getPlateauUuid(),
					command.getObserverSpeed(),
					new TwoDimensionalCoordinates(command.getAbscissa(), command.getOrdinate()));
		}
		addPlateauToContext(plateau);
	}

	void execute(RoverInitializeCommand command) {
		GameContext gameContext = GameContext.getInstance();

		// 1. loads the Plateau
		try {
			gameContext.getPlateauService().loadPlateau(command.getPlateauUuid());
		} catch (PlateauNotFoundException e) {
			throw new IllegalArgumentGameException(String.format(GameExceptionLabels.ERROR_MESSAGE_SEPARATION_PATTERN,
					e.getMessage(), GameExceptionLabels.ADDING_ROVER_NOT_ALLOWED));
		}

		// 2. initializes the rover
		gameContext.getRoverService().initializeRover(new RoverIdentifier(command.getPlateauUuid(), command.getName()),
				new TwoDimensionalCoordinates(command.getAbscissa(), command.getOrdinate()),
				Orientation.get(String.valueOf(command.getOrientation())));

		// 3. marks the Plateau location as busy
		gameContext.getPlateauService().updatePlateauWithBusyLocation(command.getPlateauUuid(),
				new TwoDimensionalCoordinates(command.getAbscissa(), command.getOrdinate()));
	}

	void execute(RoverMoveCommand command) {

		// register the subscriber for the given type of event = RoverMovedEvent
		DomainEventPublisher.instance().subscribe(new RoverMovedEventSubscriber());

		// register the subscriber in case of something went wrong during Rover moves
		DomainEventPublisher.instance().subscribe(new RoverMovedWithExceptionEventSubscriber());
		
		// register the subscriber for the plateau
		DomainEventPublisher.instance().subscribe(new PlateauSwitchedLocationEventSubscriber());

		// delegates to the rover service
		GameContext.getInstance().getRoverService().moveRoverNumberOfTimes(command.getRoverId(), command.getNumberOfMoves());

	}

	void execute(RoverTurnCommand command) {

		// register the subscriber for the given type of event = RoverMovedEvent
		DomainEventPublisher.instance().subscribe(new RoverTurnedEventSubscriber());

		// delegates to rover service
		GameContext.getInstance().getRoverService().turnRover(command.getRoverId(), command.getTurn());

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
		GameContext.getInstance().addPlateau(plateau);
	}

}
