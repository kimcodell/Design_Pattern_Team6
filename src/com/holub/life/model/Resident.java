package com.holub.life.model;

import java.util.function.Consumer;

/*** ****************************************************************
 * The Resident class implements a single cell---a "resident" of a
 * block.
 *
 * @include /etc/license.txt
 */

final class Resident implements Cell
{
	private boolean amAlive 	= false;
	private boolean willBeAlive	= false;

	/** Shared by every cell of the board; {@link #create} passes them
	 *  on, so the prototype carries the rule to the whole board.
	 */
	private final Rule				rule;
	private final EvaluationCounter evaluations;

	Resident( Rule rule, EvaluationCounter evaluations )
	{	this.rule		 = rule;
		this.evaluations = evaluations;
	}

	private boolean isStable(){return amAlive == willBeAlive; }

	/** figure the next state.
	 *  @return true if the cell is not stable (will change state on the
	 *  next transition().
	 */
	public boolean figureNextState(
							Cell north, 	Cell south,
							Cell east, 		Cell west,
							Cell northeast, Cell northwest,
							Cell southeast, Cell southwest )
	{
		verify( north, 		"north"		);
		verify( south, 		"south"		);
		verify( east, 		"east"		);
		verify( west, 		"west"		);
		verify( northeast,	"northeast"	);
		verify( northwest,	"northwest" );
		verify( southeast,	"southeast" );
		verify( southwest,	"southwest" );

		evaluations.increment();
		int neighbors = 0;

		if( north.	  isAlive()) ++neighbors;
		if( south.	  isAlive()) ++neighbors;
		if( east. 	  isAlive()) ++neighbors;
		if( west. 	  isAlive()) ++neighbors;
		if( northeast.isAlive()) ++neighbors;
		if( northwest.isAlive()) ++neighbors;
		if( southeast.isAlive()) ++neighbors;
		if( southwest.isAlive()) ++neighbors;

		willBeAlive = rule.nextState( amAlive, neighbors );	// was B3/S23 only (P5)
		return !isStable();
	}

	private void verify( Cell c, String direction )
	{	assert (c instanceof Resident) || (c == Cell.DUMMY)
				: "incorrect type for " + direction +  ": " +
				   c.getClass().getName();
	}

	/** This cell is monetary, so it's at every edge of itself. It's
	 *  an internal error for any position except for (0,0) to be
	 *  requsted since the width is 1.
	 */
	public Cell	edge(int row, int column)
	{	assert row==0 && column==0;
		return this;
	}

	public boolean transition(int rowOffset, int columnOffset,
											Consumer<Coord> changed)
	{	boolean changedState = !isStable();	// P1: was isStable(), inverted
		if( changedState )
			changed.accept( new Coord(rowOffset, columnOffset) );

		amAlive = willBeAlive;
		return changedState;
	}

	public boolean needsTransition()	{return !isStable();	}
	public boolean hasPendingEdges()	{return false;			}

	public void	   clear()			{amAlive = willBeAlive = false; }
	public boolean isAlive()		{return amAlive;			    }

	public boolean isAlive(int row, int column)
	{	assert row==0 && column==0;
		return amAlive;
	}

	public boolean set(int row, int column, boolean alive)
	{	assert row==0 && column==0;
		if( amAlive == alive )
			return false;
		amAlive = willBeAlive = alive;
		return true;
	}

	public void forEachLive(int rowOffset, int columnOffset,
											Consumer<Coord> live)
	{	if( amAlive )
			live.accept( new Coord(rowOffset, columnOffset) );
	}
	public Cell    create()			{return new Resident(rule, evaluations);	}
	public int 	   widthInCells()	{return 1;}

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL ;
	}
}
