package com.holub.life.model;

import java.util.function.Consumer;

/***
 * A group of {@link Cell} objects. Cells are grouped into neighborhoods
 * to make board updates more efficient. When a neighborhood is
 * quiescent (none of the Cells it contains are active), then it
 * ignores any requests to update itself.
 *
 * <h3>History</h3>
 * <p>11-29-04
 * Alexy Marinichev fixed the disapearing-glider problem by clearing
 * the active edges in transistion() rather then figureNextState().
 * The original call is commented out and the new line is marked 
 * with "(1)"
 *
 * @include /etc/license.txt
 */

final class Neighborhood implements Cell
{
	/** Returns true only if none of the cells in the Neighborhood
	 *  changed state during the last transition.
	 */

	private boolean amActive = false;

	/** The actual grid of Cells contained within this neighborhood. */
	private final Cell[][] grid;

	/** The neighborhood is square, so gridSize is both the horizontal
	 *  and vertical size.
	 */
	private final int 	   gridSize;

	/** Create a new Neigborhood containing gridSize-by-gridSize
	 *  clones of the prototype. The Protype is deliberately
	 *  not put into the grid, so you can reuse it if you like.
	 */

	public Neighborhood(int gridSize, Cell prototype)
	{
		this.gridSize = gridSize;
 		this.grid = new Cell[gridSize][gridSize];

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column] = prototype.create();
	}

	/** The "clone" method used to create copies of the current
	 *  neighborhood. This method is called from the containing
	 *  neighborhood's constructor. (The current neighborhood
	 *  is passed into the containing-neighborhood constructor
	 *  as the "prototype" argument.
	 */

	public Cell create()
	{	return new Neighborhood(gridSize, grid[0][0]);
	}

	/** Shows the direction of the cells along the edge of the block 
	 *  that will change  state in the next transition. For example,
	 *  if the upper-left corner has changed, then the current
	 *  Cell is disruptive in the NORTH, WEST, and NORTHWEST directions.
	 *  If this is the case, the neigboring
	 *  cells may need to be updated, even if they were previously
	 *  stable.
	 */
	public  Direction isDisruptiveTo(){ return activeEdges; }
	private	Direction activeEdges = Direction.NONE;

	/** Figures the next state of the current neighborhood and the
	 *  contained neigborhoods (or cells). Does not transition to the
	 *  next state, however. The neighboring cells are passed in as
	 *  arguments rather than stored, so a block holds no references
	 *  to its neighbors. (The original called this the Flyweight
	 *  pattern; it is not: nothing is shared to save memory. It is
	 *  plain parameter passing.)
	 *
	 *  @see #transition
	 *  @param north		The neighbor to our north
	 *  @param south		The neighbor to our south
	 *  @param east			The neighbor to our east
	 *  @param west			The neighbor to our west
	 *  @param northeast	The neighbor to our northeast
	 *  @param northwest	The neighbor to our northwest
	 *  @param southeast	The neighbor to our southeast
	 *  @param southwest	The neighbor to our southwest
	 * 
	 *  @return true if this neighborhood (i.e. any of it's cells)
	 *  			 will change state in the next transition.
	 */

	public boolean figureNextState(	Cell north, 	Cell south,
									Cell east,		Cell west,
									Cell northeast, Cell northwest,
									Cell southeast, Cell southwest )
	{
		boolean	nothingHappened = true;

		// Is some ajacent neigborhood active on the edge
		// that ajoins me?

		if(		amActive
			||	north	 .isDisruptiveTo().containsAll( Direction.SOUTH 	  )
			||	south	 .isDisruptiveTo().containsAll( Direction.NORTH 	  )
			||	east	 .isDisruptiveTo().containsAll( Direction.WEST  	  )
			||	west	 .isDisruptiveTo().containsAll( Direction.EAST  	  )
			||	northeast.isDisruptiveTo().containsAll( Direction.SOUTHWEST )
			||	northwest.isDisruptiveTo().containsAll( Direction.SOUTHEAST )
			||	southeast.isDisruptiveTo().containsAll( Direction.NORTHWEST )
			||	southwest.isDisruptiveTo().containsAll( Direction.NORTHEAST )
		)
		{
			Cell	northCell,		southCell,
					eastCell,		westCell,
					northeastCell, northwestCell,
					southeastCell, southwestCell;

			// activeEdges.clear();
					
			for( int row = 0; row < gridSize; ++row )
			{	for( int column = 0; column < gridSize; ++column )
				{
					// Get the current cell's eight neighbors

					if(row == 0 )		//{=Neighborhood.get.neighbors}
					{	northwestCell = (column==0)
							? northwest.edge(gridSize-1,gridSize-1)
							: north.edge	(gridSize-1,column-1)
							;

						northCell=	north.edge(gridSize-1,column);

						northeastCell = (column == gridSize-1 )
							? northeast.edge (gridSize-1, 0)
							: north.edge	 (gridSize-1, column+1)
							;
					}
					else
					{	northwestCell  = (column == 0)
							? west.edge(row-1, gridSize-1)
							: grid[row-1][column-1]
							;

						northCell = grid[row-1][column];

						northeastCell = (column == gridSize-1)
							? east.edge(row-1, 0)
							: grid[row-1][column+1]
							;
					}

					westCell = (column == 0)
							? west.edge( row, gridSize-1)
							: grid[row][column-1]
							;

					eastCell = (column == gridSize-1)
							? east.edge(row, 0)
							: grid[row][column+1]
							;

					if(row == gridSize-1)
					{	southwestCell = ( column==0 )
							? southwest.edge(0,gridSize-1)
							: south.edge(0,column-1)
							;

						southCell = south.edge(0,column);

						southeastCell = (column == gridSize-1 )
							? southeast.edge(0,0)
							: south.edge(0, column+1)
							;
					}
					else
					{	southwestCell  = (column == 0)
							? west.edge(row+1, gridSize-1)
							: grid[row+1][column-1]
							;

						southCell = grid[row+1][column];

						southeastCell = (column == gridSize-1)
							? east.edge(row+1, 0)
							: grid[row+1][column+1]
							;
					}

					// Tell the cell to change its state. If
					// the cell changed (the figureNextState request
					// returned false), then mark the current block as
					// unstable. Also, if the unstable cell is on the
					// edge of the block modify activeEdges to
					//  indicate which edge or edges changed. 

					if( grid[row][column].figureNextState
						( northCell, 	 southCell,
						  eastCell,	  	 westCell,
						  northeastCell, northwestCell,
						  southeastCell, southwestCell
						)
					)
					{   nothingHappened = false;
					}
				}
			}
		}

		amActive = !nothingHappened;
		return amActive;
	}


	/** Transition the neighborhood to the previously-computed
	 *  state.
	 *  @return true if the transition actually changed anything.
	 *  @see #figureNextState
	 */
	public boolean transition(int rowOffset, int columnOffset,
											Consumer<Coord> changed)
	{	
		// No locking: the board is used from one thread only
		// (the event-dispatch thread), so nobody can read it while
		// it is in the middle of a transition. The original static
		// lock was shared by every board and was never released if
		// a transition threw an exception.

		boolean someSubcellChangedState = false;

		activeEdges = Direction.NONE;					/*(1)*/

		int subcellWidth = grid[0][0].widthInCells();

		boolean subcellEdgesPending = false;

		for( int row = 0; row < gridSize; ++row ) //{=transition.start}
			for( int column = 0; column < gridSize; ++column )
			{	Cell subcell = grid[row][column];

				// A quiet subcell with no edge markers to clear has
				// nothing to do, so the whole idle part of the board
				// is skipped.
				if( !subcell.needsTransition() )
					continue;

				if( subcell.transition(
								rowOffset    + row    * subcellWidth,
								columnOffset + column * subcellWidth,
								changed ) )
				{	rememberThatCellAtEdgeChangedState(row, column);
					someSubcellChangedState = true;
				}								 //{=transition.end}

				subcellEdgesPending |= subcell.hasPendingEdges();
			}

		edgesPending = !activeEdges.isEmpty()
					   || subcellEdgesPending;
		return someSubcellChangedState;
	}

	/** True if activeEdges (here or in a subcell) may be non-empty,
	 *  so a transition is needed to clear them even when nothing
	 *  changes. Without this, edge markers survive a generation in
	 *  which nothing changed and keep waking neighbors (N1).
	 */
	private boolean edgesPending = false;

	public boolean needsTransition()	{return amActive || edgesPending;	}
	public boolean hasPendingEdges()	{return edgesPending;				}


	/** Modifies activeEdges to indicate whether the addition
	 *  of the cell at (row,column) makes an edge active.
	 */
	private void rememberThatCellAtEdgeChangedState(int row,int column)
	{
		if( row == 0 )
		{	activeEdges = activeEdges.plus( Direction.NORTH );

			if(column==0)
				activeEdges = activeEdges.plus( Direction.NORTHWEST );
			else if(column==gridSize-1)
				activeEdges = activeEdges.plus( Direction.NORTHEAST );
		}
		else if( row == gridSize-1 )
		{	activeEdges = activeEdges.plus( Direction.SOUTH );

			if(column==0)
				activeEdges = activeEdges.plus( Direction.SOUTHWEST );
			else if(column==gridSize-1)
				activeEdges = activeEdges.plus( Direction.SOUTHEAST );
		}

		if( column == 0 )
		{	activeEdges = activeEdges.plus( Direction.WEST );
		}
		else if( column == gridSize-1 )
		{	activeEdges = activeEdges.plus( Direction.EAST );
		}
		// else it's an internal cell. Do nothing.
	}

	/** Return the edge cell in the indicated row and column.
	 */
	public Cell	edge(int row, int column)
	{	assert 	  (row	  == 0 || row	 == gridSize-1)
			   || (column == 0 || column == gridSize-1)
			   :  "central cell requested from edge()";

		return grid[row][column];
	}

	/** Set the cell at (row, column), relative to this block, and
	 *  mark this block (and the edge the cell is on) active, exactly
	 *  as the original mouse-click handler did. Every path that
	 *  changes a cell goes through here, so the "changed cells are
	 *  marked active" invariant holds for clicks, edits and restores.
	 */
	public boolean set(int row, int column, boolean alive)
	{	int subcellWidth = grid[0][0].widthInCells();
		int subRow		 = row	  / subcellWidth;
		int subColumn	 = column / subcellWidth;

		if( !grid[subRow][subColumn].set( row	 % subcellWidth,
										  column % subcellWidth, alive ) )
			return false;

		amActive	 = true;
		edgesPending = true;
		rememberThatCellAtEdgeChangedState(subRow, subColumn);
		return true;
	}

	public boolean isAlive(int row, int column)
	{	int subcellWidth = grid[0][0].widthInCells();
		return grid[row / subcellWidth][column / subcellWidth]
					.isAlive( row % subcellWidth, column % subcellWidth );
	}

	public void forEachLive(int rowOffset, int columnOffset,
											Consumer<Coord> live)
	{	int subcellWidth = grid[0][0].widthInCells();
		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column].forEachLive(
								rowOffset    + row    * subcellWidth,
								columnOffset + column * subcellWidth,
								live );
	}

	/** True if any cell in this block is alive. (The original
	 *  always returned true, P10.)
	 */
	public boolean isAlive()
	{	for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				if( grid[row][column].isAlive() )
					return true;
		return false;
	}

	public int widthInCells()
	{	return gridSize * grid[0][0].widthInCells();
	}

	public void clear()
	{	activeEdges = Direction.NONE;

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column].clear();

		amActive	 = false;
		edgesPending = false;
	}
}
