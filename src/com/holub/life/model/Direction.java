package com.holub.life.model;

/***
 *  The Direction class is used to indicate in just what way a
 *  particular Cell is unstable. If a subcell has just changed
 *  on the north side, for example, then you indicate the
 *  change by issuing:
 *  <PRE>
 *  edges = edges.plus( Direction.NORTH );
 *  </PRE>
 *  Later on, when updating, you can check whether a cell
 *  is disruptive on a particular edge by issuing:
 *  <PRE>
 *  if( someCell.isDisruptiveTo().containsAll( Direction.NORTH ) )
 *      //...
 *  </PRE>
 *
 *  <p>Directions are immutable values. The original was a mutable
 *  class whose constants were an "Immutable" subclass that refused
 *  add() and clear(), which broke substitutability (LSP); it also had
 *  no equals(Object)/hashCode, and a cell handed out its own mutable
 *  Direction (P8, P9). There are only 256 combinations, so every
 *  value is created once and reused.
 *
 * @include /etc/license.txt
 */

final class Direction
{
	private static final int BITS_NORTH     = 0x0001;
	private static final int BITS_SOUTH     = 0x0002;
	private static final int BITS_EAST      = 0x0004;
	private static final int BITS_WEST      = 0x0008;
	private static final int BITS_NORTHEAST = 0x0010;
	private static final int BITS_NORTHWEST = 0x0020;
	private static final int BITS_SOUTHEAST = 0x0040;
	private static final int BITS_SOUTHWEST = 0x0080;
	private static final int BITS_ALL       = 0x00ff;
	private static final int BITS_NONE      = 0x0000;

	private static final Direction[] VALUES = new Direction[BITS_ALL + 1];
	static
	{   for( int bits = 0; bits <= BITS_ALL; ++bits )
			VALUES[bits] = new Direction(bits);
	}

	// Various directions. Note that, since we're talking
	// about the edges of a grid, that NORTH | WEST and NORTHWEST are
	// different things. NORTH means that anything along the NORTH
	// edge is active, ditto for WEST and the west edge. NORTHWEST
	// means that the cell in the NORTHWEST corner is active.
	// If the NORTHWEST corner is active, the NORTH and WEST
	// edges will also be active, but the converse is not true.

	static final Direction NORTH     = VALUES[BITS_NORTH];
	static final Direction SOUTH     = VALUES[BITS_SOUTH];
	static final Direction EAST      = VALUES[BITS_EAST];
	static final Direction WEST      = VALUES[BITS_WEST];
	static final Direction NORTHEAST = VALUES[BITS_NORTHEAST];
	static final Direction NORTHWEST = VALUES[BITS_NORTHWEST];
	static final Direction SOUTHEAST = VALUES[BITS_SOUTHEAST];
	static final Direction SOUTHWEST = VALUES[BITS_SOUTHWEST];
	static final Direction ALL       = VALUES[BITS_ALL];
	static final Direction NONE      = VALUES[BITS_NONE];

	private final int bits;

	private Direction( int bits ){  this.bits = bits;  }

	/** This direction combined with another. */
	Direction plus( Direction d )       { return VALUES[bits | d.bits];      }

	/** True if every edge in d is also in this direction. */
	boolean containsAll( Direction d )  { return (bits & d.bits) == d.bits;  }

	/** True if this direction and d share at least one edge. */
	boolean intersects( Direction d )   { return (bits & d.bits) != 0;       }

	boolean isEmpty()                   { return bits == BITS_NONE;          }

	@Override public boolean equals( Object other )
	{   return other instanceof Direction d && d.bits == bits;
	}

	@Override public int hashCode()     { return bits;                       }

	@Override public String toString()  { return "Direction[" + Integer.toBinaryString(bits) + "]"; }
}
