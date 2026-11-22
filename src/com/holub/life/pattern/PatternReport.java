package com.holub.life.pattern;

import java.util.*;

/** Everything found on a board at one generation (immutable). */
public record PatternReport(long generation, List<Detection> detections)
{
	public static final PatternReport EMPTY = new PatternReport(0, List.of());

	/** Detections with the same name and group, counted together. */
	public record Group(String name, PatternCategory category, Detection.Source source,
						List<Detection> detections)
	{
		public Group
		{
			detections = List.copyOf(detections);
		}

		public int count() { return detections.size(); }
	}

	public PatternReport
	{
		detections = List.copyOf(detections);
	}

	/** Classified detections grouped by category, in category order;
	 *  every category is present. Inside a category, the most numerous
	 *  pattern comes first.
	 */
	public Map<PatternCategory, List<Group>> grouped()
	{
		Map<PatternCategory, List<Group>> result = new EnumMap<>(PatternCategory.class);
		for (PatternCategory category : PatternCategory.values())
			result.put(category, groupsOf(detections.stream()
								 .filter(d -> d.category() == category).toList()));
		return Collections.unmodifiableMap(result);
	}

	/** Detections that fit no category, grouped by name. */
	public List<Group> unclassified()
	{
		return groupsOf(detections.stream().filter(d -> !d.isClassified()).toList());
	}

	/** Number of classified detections in a category. */
	public int count(PatternCategory category)
	{
		return (int) detections.stream().filter(d -> d.category() == category).count();
	}

	private static List<Group> groupsOf(List<Detection> list)
	{
		Map<String, List<Detection>> byName = new LinkedHashMap<>();
		for (Detection d : list)
			byName.computeIfAbsent(d.name(), name -> new ArrayList<>()).add(d);
		List<Group> groups = new ArrayList<>();
		byName.forEach((name, same) ->
			groups.add(new Group(name, same.get(0).category(), same.get(0).source(), same)));
		groups.sort(Comparator.comparingInt(Group::count).reversed().thenComparing(Group::name));
		return List.copyOf(groups);
	}
}
