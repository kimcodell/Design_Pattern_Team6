package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.BoardView;

/** Finds the patterns on a board and groups them (Facade over the
 *  detection stages). Reads the board only.
 *
 *  <p>Stages, in order: exact catalog shapes ({@link CatalogMatcher},
 *  skipped if the board's rule is not the catalog's), then grouping by
 *  behavior ({@link BehaviorClassifier}), then everything left as
 *  debris ({@link Unclassified}).
 *
 *  <p>Not thread-safe: the behavior cache is shared between calls.
 */
public final class PatternAnalyzer
{
	private final PatternCatalog catalog;
	private final List<DetectionStage> stages;

	public PatternAnalyzer(PatternCatalog catalog)
	{
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.stages  = List.of(new CatalogMatcher(catalog),
							   new BehaviorClassifier(),
							   new Unclassified());
	}

	public PatternCatalog catalog() { return catalog; }

	/** True if the catalog is used for this board (same rule). */
	public boolean usesCatalog(BoardView board)
	{
		return board.rule().equals(catalog.rule());
	}

	public PatternReport analyze(BoardView board)
	{
		CellClaims claims = new CellClaims(board);
		List<Detection> found = new ArrayList<>();
		for (DetectionStage stage : stages)
			if (stage.appliesTo(board))
				found.addAll(stage.detect(claims));
		return new PatternReport(board.generation(), found);
	}
}
