package com.game.domain.application;

import com.game.domain.application.command.plateau.PlateauInitializeCommand;
import com.game.domain.application.command.rover.RoverInitializeCommand;
import com.game.domain.application.command.rover.RoverMoveCommand;
import com.game.domain.application.command.rover.RoverTurnCommand;

public class CommandVisitor {
	
	private GameServiceImpl gameService = new GameServiceImpl();
	
	public void visit(PlateauInitializeCommand command) {
		gameService.execute(command);
	}
	
	public void visit(RoverInitializeCommand command) {
		gameService.execute(command);
	}
	
	public void visit(RoverMoveCommand command) {
		gameService.execute(command);
	}
	
	public void visit(RoverTurnCommand command) {
		gameService.execute(command);
	}

}
