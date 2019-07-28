package com.empire;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

class SpyRing extends RulesObject {
	static SpyRing newSpyRing(Rules rules) { // For GSON.
		return newSpyRing(rules, null, 0, 0);
	}

	static SpyRing newSpyRing(Rules rules, String nation, double strength, int location) {
		return new SpyRing(rules, nation, strength, location);
	}

	enum InvolvementDisposition {
		SABOTAGING,
		SUPPORTING,
	}

	private String nation;
	private int location;
	private double strength;
	private boolean hidden = true;
	private Integer involvedInPlotId; // null indicates no involvement.
	private InvolvementDisposition involvementType;

	private double calcPlotPower(World w, Region target) {
		final class Node {
			public final double power;
			public final Region location;
			public Node(double power, Region location) {
				this.power = power;
				this.location = location;
			}
		}

		Function<Node, Node> getPower = n -> {
			Region r = n.location;
			if (r.isSea()) return new Node(n.power * getRules().plotDecaySea, n.location);
			double unrestFactor = NationData.isFriendly(nation, r.getKingdom(), w) ? 1 - r.calcUnrest(w) : r.calcUnrest(w);
			return new Node(n.power * (getRules().plotDecayMin + unrestFactor * (getRules().plotDecayMax - getRules().plotDecayMin)), n.location);
		};

		PriorityQueue<Node> pq = new PriorityQueue<>(100, Comparator.comparingDouble(n -> -n.power));
		Set<Region> visited = new HashSet<>();
		pq.add(getPower.apply(new Node(strength, w.regions.get(location))));
		while (!pq.isEmpty()) {
			Node n = pq.poll();
			if (visited.contains(n.location)) continue;
			visited.add(n.location);
			if (n.location == target) {
				return n.power;
			}
			for (Region r : n.location.getNeighbors(w)) {
				if (!visited.contains(r)) pq.add(getPower.apply(new Node(n.power, r)));
			}
		}
		return 0;
	}

	public String getNation() {
		return nation;
	}

	public int getLocation() {
		return location;
	}

	public void expose() {
		hidden = false;
	}

	public boolean isExposed() {
		return !hidden;
	}

	public void damage() {
		strength *= getRules().spyRingDamageFactor;
	}

	public void involve(int plotId, InvolvementDisposition involvement) {
		involvedInPlotId = plotId;
		involvementType = involvement;
	}

	Optional<InvolvementDisposition> getInvolvementIn(int plotId) {
		if (involvedInPlotId == null) return Optional.empty();
		return plotId == involvedInPlotId ? Optional.of(involvementType) : Optional.empty();
	}

	void addContributionTo(int plotId, Region targetRegion, String defender, World w, Plot.OutcomeWeights outcome) {
		if (!nation.equals(defender) && (involvedInPlotId == null || involvedInPlotId != plotId)) return;
		double strength = calcPlotPower(w, targetRegion);
		if (nation.equals(defender)) {
			outcome.defend(strength);
		} else if (plotId == involvedInPlotId && involvementType == InvolvementDisposition.SUPPORTING) {
			outcome.support(strength);
		} else if (plotId == involvedInPlotId && involvementType == InvolvementDisposition.SABOTAGING) {
			double mod = 1;
			if (NationData.getStateReligion(nation, w) == Ideology.LYSKR) mod += getRules().lyskrSabotageMod;
			outcome.sabotage(strength * mod);
		}
	}

	boolean belongsTo(String kingdom) {
		return nation.equals(kingdom);
	}

	public void grow() {
		strength += (getRules().spyRingMaxStrength - strength) * getRules().spyRingGrowthFactor;
	}

	private SpyRing(Rules rules, String nation, double strength, int location) {
		super(rules);
		this.nation = nation;
		this.strength = strength;
		this.location = location;
	}
}
