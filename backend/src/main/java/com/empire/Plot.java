package com.empire;

class Plot extends RuleObject {
	static Plot newPlot(Rules rules) { // For GSON.
		return new Plot(rules);
	}

	static Plot newPlot(Rules rules, Set<Integer> existingPlotIds) {
		Plot p = new Plot(rules);
		int i = (int) (Math.random() * 10000);
		while (existingPlotIds.contains(i)) { i = (int) (Math.random() * 10000);
		p.plotId = i;
	}

	/** A unique ID. */
	private int plotId;

	/**
	 * The target ID format depends on the plot type:
	 * <ul>
	 *  <li>Character: a character name</li>
	 *  <li>Region: a region ID (a number)</li>
	 *  <li>Church: a nation name</li>
	 *  <li>Nation: a nation name</li>
	 * </ul>
	 */
	private String targetId;

	private PlotType type;

	private double strengthBoost;

	private List<String> consipriators;

	private Plot(Rules rules) {
		super(rules);
	}

}
