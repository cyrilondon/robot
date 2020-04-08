package com.game.domain.application;

import java.util.UUID;

import com.game.domain.application.command.InitializeRoverCommand;
import com.game.domain.application.command.MoveRoverCommand;
import com.game.domain.model.entity.dimensions.TwoDimensionalCoordinates;

/**
 * 
 *
 */
public interface GameService extends ApplicationService {

	/**
	 * TODO replace the arguments by command object
	 * 
	 * @param coordinates
	 */
	public void initializePlateau(UUID uuid, TwoDimensionalCoordinates coordinates);

	/**
	 * 
	 * @param speed
	 * @param coordinates
	 */
	public void initializeRelativisticPlateau(UUID uuid, int speed, TwoDimensionalCoordinates coordinates);

	/**
	 * Initialize the rover
	 * @param coordinates
	 * @param orientation
	 */
	public void execute(InitializeRoverCommand command);

	/**
	 * Moves the rover
	 * 
	 * @param roverName
	 * @param orientation
	 */
	public void execute(MoveRoverCommand command);

}
