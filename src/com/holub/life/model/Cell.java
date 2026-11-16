package com.holub.life.model;

import java.util.function.Consumer;

/***
 * This interface is the basic unit that comprises a life board.
 * It's implemented both by {@link Resident} (which represents
 * an individual cell on the board) and {@link Neighborhood},
 * which represents a group of cells.
 *
 * <p>Drawing, mouse handling and storage were removed from this
 * interface: the view reads the board through {@link LifeModel}, and
 * storage goes through {@link BoardSnapshot}. Cell positions inside a
 * composite are addressed with (row, column) relative to the cell's
 * upper-left corner.
 *
 * @include /etc/license.txt
 */

interface Cell
{
	/** Figure out the next state of the cell, given the specified
	 *  neighbors.
	 *  @return true if the cell is unstable (changed state).
	 */
	boolean figureNextState(    Cell north,     Cell south,
								Cell east,      Cell west,
								Cell northeast, Cell northwest,
								Cell southeast, Cell southwest );

	/** Access a specific contained cell located at the edge of the
	 *  composite cell.
	 *  @param row      The requested row. Must be on the edge of
	 *                  the block.
	 *  @param column   The requested column. Must be on the edge
	 *                  of the block.
	 *  @return the cell at that edge position.
	 */
	Cell edge( int row, int column );

	/** Transition to the state computed by the most recent call to
	 *  {@link #figureNextState}
	 *  @param rowOffset    Board row of this cell's upper-left corner.
	 *  @param columnOffset Board column of this cell's upper-left corner.
	 *  @param changed      Receives the board position of every cell
	 *                      that changed state.
	 *  @return true if a changed of state happened during the transition.
	 */
	boolean transition( int rowOffset, int columnOffset, Consumer<Coord> changed );

	/** Return true if {@link #transition} has anything to do: some
	 *  cell will change state, or edge markers from an earlier
	 *  generation must be cleared.
	 */
	boolean needsTransition();

	/** Return true if this cell or a subcell still has edge markers
	 *  that a later transition has to clear.
	 */
	boolean hasPendingEdges();

	/** Return true if this cell or any subcells are alive.
	 */
	boolean isAlive();

	/** Return true if the cell at (row, column), relative to this
	 *  cell's upper-left corner, is alive.
	 */
	boolean isAlive( int row, int column );

	/** Set the cell at (row, column) to the given state and mark the
	 *  enclosing blocks (and their edges) active so the next
	 *  generation is computed correctly.
	 *  @return true if the cell's state actually changed.
	 */
	boolean set( int row, int column, boolean alive );

	/** Report the board position of every live cell.
	 */
	void forEachLive( int rowOffset, int columnOffset, Consumer<Coord> live );

	/** Return the specified width plus the current cell's width
	 */
	int widthInCells();

	/** Return a fresh (newly created) object identical to yourself
	 *  in content.
	 */
	Cell create();

	/** Returns a Direction indicated the directions of the cells
	 *  that have changed state.
	 *  @return A Direction object that indicates the edge or edges
	 *          on which a change has occured.
	 */

	Direction isDisruptiveTo();

	/** Set the cell and all subcells into a "dead" state.
	 */

	void clear();

	/** The DUMMY Singleton represents a permanently dead (thus stable)
	 *  cell. It's used for the edges of the grid. It's a singleton.
	 *  The Dummy class is private, but it is accessed through
	 *  the public DUMMY field, declared below. I'd like this
	 *  class to be private, but the JLS doesn't allow private
	 *  members in an interface.
	 */

	public static final Cell DUMMY = new Cell()
	{
		public boolean figureNextState(Cell n,  Cell s,  Cell e,  Cell w,
									   Cell ne, Cell nw, Cell se, Cell sw)
											{return false;            }	// stable (P12)

		public Cell      edge(int r, int c) {return this;             }
		public boolean   isAlive()          {return false;            }
		public boolean   isAlive(int r, int c) {return false;         }
		public Cell      create()           {return this;             }
		public Direction isDisruptiveTo()   {return Direction.NONE;   }
		public void      clear()            {                         }
		public int       widthInCells()     {return 0;                }
		public boolean   needsTransition()  {return false;            }
		public boolean   hasPendingEdges()  {return false;            }

		public boolean transition(int r, int c, Consumer<Coord> changed)
		{   return false;
		}

		public boolean set(int r, int c, boolean alive)
		{   throw new UnsupportedOperationException(
						"Cannot set a cell outside the board");
		}

		public void forEachLive(int r, int c, Consumer<Coord> live)
		{
		}
	};
}
