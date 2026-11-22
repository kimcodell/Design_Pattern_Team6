package com.holub.life.pattern;

import java.util.List;
import com.holub.life.model.BoardView;

/** One step of pattern analysis (Chain of Responsibility). Stages run
 *  in order; each explains some of the cells that earlier stages left
 *  unclaimed and passes the rest on.
 */
interface DetectionStage
{
	/** False if this stage cannot work on the board (for example, a
	 *  catalog made for a different rule). The stage is then skipped.
	 */
	default boolean appliesTo(BoardView board) { return true; }

	/** Find patterns among the unclaimed cells, and claim their cells. */
	List<Detection> detect(CellClaims claims);
}
