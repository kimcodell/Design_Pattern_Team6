package com.holub.life.pattern;

import java.util.*;
import com.holub.life.model.Coord;

/** The end of the chain: every cluster still unclaimed is reported as
 *  debris, so that the report accounts for every live cell.
 */
final class Unclassified implements DetectionStage
{
	static final String NAME = "Debris";

	@Override public List<Detection> detect(CellClaims claims)
	{
		List<Detection> found = new ArrayList<>();
		for (CellClaims.Cluster cluster : claims.unclaimedClusters())
		{
			Shape shape = new Shape(cluster.unwrapped());
			cluster.cells().forEach(claims::claim);
			found.add(new Detection(NAME, null, Detection.Source.UNCLASSIFIED, 0,
									shape.top(), shape.left(), shape.height(), shape.width(),
									cluster.cells()));
		}
		return found;
	}
}
