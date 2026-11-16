package com.holub.life.model;

/** A cell position on the board. Row 0 is the top row and column 0
 *  is the left column. The model uses this instead of java.awt.Point
 *  so that it does not depend on AWT.
 */
public record Coord(int row, int col)
{
}
