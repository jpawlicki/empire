package com.empire;

import com.empire.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

abstract class Plot {
	public static void plot(World w, String perpetrator, Map<String, String> plotParameters, Map<String, World.Budget> incomeSources) {
		Plot p = getPlot(w, new Parameters(w, plotParameters, perpetrator));
		if (p == null) return;
		if (p.isValid()) {
			boolean canPay = true;
			Nation perp = w.getNation(perpetrator);
			for (String kingdom : w.getNationNames()) if (p.getCost(kingdom) > perp.getLeverage(kingdom)) canPay = false;
			if (canPay) {
				for (String kingdom : w.getNationNames()) perp.addLeverage(w, kingdom, -p.getCost(kingdom), 1);
				w.notifyPlayer(perpetrator, "Plot", "Our plot to " + p.getDescription() + " was successful.");
				p.actualize(w, incomeSources);
			} else {
				w.notifyPlayer(perpetrator, "Plot", "We lacked sufficient leverage to enact a plot to " + p.getDescription());
			}
		} else {
			w.notifyPlayer(perpetrator, "Plot", "Our plot to " + p.getDescription() + " was invalidated.");
		}
	}

	private static Plot getPlot(World w, Parameters p) {
		switch (p.type) {
			case "assassinate": return new Assassinate(p);
			case "conceal": return new Conceal(p);
			case "convert": return new Convert(p);
			case "denounce": return new Denounce(p, w);
			case "destroy": return new Destroy(p);
			case "hobble": return new Hobble(p);
			case "incite_popular": return new IncitePopular(p);
			case "incite_noble": return new InciteNoble(p);
			case "intercept": return new Intercept(p);
			case "praise": return new Praise(p, w);
			case "ruin": return new Ruin(p);
			case "smuggle": return new Smuggle(p);
			case "steal": return new Steal(p);
			case "rebel": return new Rebel(p, w);
		}
		return null;
	}

	protected static class Parameters {
		public final String perpetrator;
		public final String type;
		public final Optional<Character> character;
		public final Optional<Army> army;
		public final Optional<Nation> nation;
		public final Optional<Region> region;
		public final Optional<Region> destination;
		public final Optional<Double> amount;
		public final Optional<Ideology> ideology;
		public final Optional<Predicate<Construction>> construction;

		public Parameters(World w, Map<String, String> parameters, String perpetrator) {
			this.perpetrator = perpetrator;

			this.type = parameters.get("type");

			character = w.getCharacterByName(parameters.get("character"));

			Optional<Army> army;
			try {
				army = w.getArmyById(Integer.parseInt(parameters.get("army")));
			} catch (NumberFormatException e) {
				army = Optional.empty();
			}
			this.army = army;

			nation = Optional.ofNullable(w.getNation(parameters.get("nation")));
		
			Optional<Region> region;
			try {
				region = Optional.ofNullable(w.regions.get(Integer.parseInt(parameters.get("region"))));
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				region = Optional.empty();
			}
			this.region = region;
			
			Optional<Region> destination;
			try {
				destination = Optional.ofNullable(w.regions.get(Integer.parseInt(parameters.get("destination"))));
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				destination = Optional.empty();
			}
			this.destination = destination;

			Optional<Double> amount;
			try {
				amount = Optional.of(Double.valueOf(parameters.get("amount")));
			} catch (NumberFormatException | NullPointerException e) {
				amount = Optional.empty();
			}
			this.amount = amount;

			ideology = Optional.ofNullable(Ideology.fromString(parameters.get("ideology")));

			construction = Optional.of(c -> parameters.getOrDefault("construction", "").startsWith(c.type.toString().toLowerCase()) && (c.type != Construction.Type.TEMPLE || parameters.getOrDefault("construction", "").endsWith(c.religion.toString())));
		}
	}

	protected final Parameters parameters;

	protected abstract boolean isValid();
	protected abstract double getCost(String kingdom);
	protected abstract String getDescription();
	protected abstract void actualize(World w, Map<String, World.Budget> budgets);

	private Plot(Parameters parameters) {
		this.parameters = parameters;
	}

	private static class Assassinate extends Plot {
		Assassinate(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.character.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return parameters.character.get().kingdom.equals(kingdom) ? 50 : 0;
		}

		@Override
		protected String getDescription() {
			Character c = parameters.character.get();
			return "assassinate " + c.name + " (a hero of " + c.kingdom + ")";
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			Character target = parameters.character.get();
			w.characters.remove(target);
			w.makeNewCharacter(target.kingdom);
			w.notifyAllPlayers("Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Conceal extends Plot {
		Conceal(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.army.isPresent() && parameters.nation.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.nation.get().name) ? parameters.army.get().getEffectiveSize() / 100 : 0;
		}

		@Override
		protected String getDescription() {
			return "conceal " + (parameters.army.get().type == Army.Type.ARMY ? "army " : "navy ") + parameters.army.get().id + " from " + parameters.nation.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.army.get().concealedFrom.add(parameters.nation.get().name);
		}
	}

	private static class Convert extends Plot {
		Convert(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			if (!parameters.region.isPresent() || !parameters.ideology.isPresent()) return false;
			if (parameters.ideology.get() == Ideology.COMPANY) return false;
			return
				parameters.region.get().constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && c.religion == parameters.ideology.get()).count()
				== parameters.region.get().constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && c.religion == parameters.region.get().religion).count();
		}
		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.region.get().getKingdom()) ? 10 : 0;
		}

		@Override
		protected String getDescription() {
			return "convert " + parameters.region.get().name + " to " + parameters.ideology.get();
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.region.get().setReligion(parameters.ideology.get(), w);
			w.notifyPlayer(parameters.region.get().getKingdom(), "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Denounce extends Plot {
		final String holyCityController;

		Denounce(Parameters p, World w) {
			super(p);
			holyCityController = w.regions.get(w.getGeography().holycity).getKingdom();
		}

		@Override
		protected boolean isValid() {
			return parameters.nation.isPresent() && parameters.amount.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(holyCityController) ? parameters.amount.get() / 3 : 0;
		}

		@Override
		protected String getDescription() {
			return "denounce " + parameters.nation.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.nation.get().addGoodwill(-parameters.amount.get());
			HashSet<String> notifiers = new HashSet<>();
			notifiers.add(parameters.nation.get().name);
			notifiers.add(holyCityController);
			for (String n : notifiers) w.notifyPlayer(n, "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Destroy extends Plot {
		Destroy(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.region.isPresent() && parameters.construction.isPresent() && parameters.region.get().constructions.stream().filter(parameters.construction.get()).count() > 0;
		}
		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.region.get().getKingdom()) ? 12 : 0;
		}

		@Override
		protected String getDescription() {
			return "destroy a construction in " + parameters.region.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			Optional<Construction> destroyed = parameters.region.get().constructions.stream().filter(parameters.construction.get()).max(Comparator.comparing(c -> c.originalCost));
			parameters.region.get().constructions.remove(destroyed.get());
			w.notifyPlayer(parameters.region.get().getKingdom(), "Plot", "A plot to destroy a " + destroyed.get().type.toString().toLowerCase() + " in " + parameters.region.get().name + " was successful.");
		}
	}

	private static class Hobble extends Plot {
		Hobble(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.army.isPresent() && !Nation.PIRATE_NAME.equals(parameters.army.get().kingdom);
		}

		@Override
		protected double getCost(String kingdom) {
			return parameters.army.get().kingdom.equals(kingdom) ? parameters.army.get().getEffectiveSize() / 100 : 0;
		}

		@Override
		protected String getDescription() {
			return "hobble the efficacy of " + parameters.army.get().getName();
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.army.get().hobbles++;
			w.notifyPlayer(parameters.army.get().kingdom, "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class IncitePopular extends Plot {
		IncitePopular(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.region.isPresent() && parameters.amount.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.region.get().getKingdom()) ? parameters.amount.get() / 3 : 0;
		}

		@Override
		protected String getDescription() {
			return "incite popular unrest in " + parameters.region.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.region.get().unrestPopular.add(parameters.amount.get());
			w.notifyPlayer(parameters.region.get().getKingdom(), "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class InciteNoble extends Plot {
		InciteNoble(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.region.isPresent() && parameters.amount.isPresent() && parameters.region.get().noble != null;
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.region.get().getKingdom()) ? parameters.amount.get() / 3 : 0;
		}

		@Override
		protected String getDescription() {
			return "incite popular unrest in " + parameters.region.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.region.get().noble.unrest.add(parameters.amount.get());
			w.notifyPlayer(parameters.region.get().getKingdom(), "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Intercept extends Plot {
		Intercept(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.nation.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.nation.get().name) ? 15 : 0;
		}

		@Override
		protected String getDescription() {
			return "intercept the communications of " + parameters.nation.get();
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			w.communications
				.stream()
				.filter(c -> c.postDate == w.date)
				.filter(c -> c.from.equals(parameters.nation.get().name) || c.to.contains(parameters.nation.get().name))
				.forEach(c -> c.intercepted.add(parameters.perpetrator));
		}
	}

	private static class Praise extends Plot {
		final String holyCityController;

		Praise(Parameters p, World w) {
			super(p);
			holyCityController = w.regions.get(w.getGeography().holycity).getKingdom();
		}

		@Override
		protected boolean isValid() {
			return parameters.nation.isPresent() && parameters.amount.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(holyCityController) ? parameters.amount.get() / 5 : 0;
		}

		@Override
		protected String getDescription() {
			return "praise " + parameters.nation.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.nation.get().addGoodwill(parameters.amount.get());
			HashSet<String> notifiers = new HashSet<>();
			notifiers.add(parameters.nation.get().name);
			notifiers.add(holyCityController);
			for (String n : notifiers) w.notifyPlayer(n, "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Ruin extends Plot {
		Ruin(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.region.isPresent() && parameters.amount.isPresent() && parameters.amount.get() > 0;
		}

		@Override
		protected double getCost(String kingdom) {
			return parameters.region.get().getKingdom().equals(kingdom) ? parameters.amount.get() / 20 : 0;
		}

		@Override
		protected String getDescription() {
			return "ruin " + Math.round(parameters.amount.get()) + "k crops in " + parameters.region.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.region.get().crops = Math.max(0, parameters.region.get().crops - parameters.amount.get() * 1000);
			w.notifyPlayer(parameters.region.get().getKingdom(), "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Smuggle extends Plot {
		Smuggle(Parameters p) { super(p); }

		@Override
		protected boolean isValid() {
			return parameters.region.isPresent() && parameters.destination.isPresent() && parameters.amount.isPresent() && parameters.amount.get() > 0;
		}

		@Override
		protected double getCost(String kingdom) {
			return parameters.region.get().getKingdom().equals(kingdom) ? parameters.amount.get() / 10 : 0;
		}

		@Override
		protected String getDescription() {
			return "smuggle " + Math.round(parameters.amount.get()) + "k measures of food from " + parameters.region.get().name + " to " + parameters.destination.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			if (!parameters.region.get().canFoodTransferTo(w, parameters.destination.get())) return;
			double amount = Math.min(parameters.region.get().food, parameters.amount.get() * 1000);
			parameters.region.get().food -= amount;
			parameters.destination.get().food += amount;
			HashSet<String> notifiers = new HashSet<>();
			notifiers.add(parameters.region.get().getKingdom());
			notifiers.add(parameters.destination.get().getKingdom());
			for (String n : notifiers) w.notifyPlayer(n, "Plot", "A plot to " + getDescription() + " was successful.");
		}
	}

	private static class Steal extends Plot {
		final double actualTheft;

		Steal(Parameters p) {
			super(p);
			if (!isValid()) {
				actualTheft = 0;
			} else {
				actualTheft = Math.min(parameters.amount.get(), parameters.nation.get().gold);
			}
		}

		@Override
		protected boolean isValid() {
			return parameters.nation.isPresent() && parameters.amount.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.nation.get().name) ? actualTheft / 4 : 0;
		}

		@Override
		protected String getDescription() {
			return "steal " + Math.round(actualTheft) + " gold from " + parameters.nation.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			parameters.nation.get().gold -= actualTheft;
			w.getNation(parameters.perpetrator).gold += actualTheft;
			w.notifyPlayer(parameters.nation.get().name, "Plot", "A plot to " + getDescription() + " was successful.");
			budgets.get(parameters.perpetrator).incomeIntrigue += actualTheft;
			budgets.get(parameters.nation.get().name).spentIntrigue += actualTheft;
		}
	}

	private static class Rebel extends Plot {
		private World w;

		Rebel(Parameters p, World w) {
			super(p);
			this.w = w;
		}

		@Override
		protected boolean isValid() {
			return parameters.nation.isPresent() && parameters.region.isPresent();
		}

		@Override
		protected double getCost(String kingdom) {
			return kingdom.equals(parameters.region.get().getKingdom()) ? 60 - parameters.region.get().calcUnrest(w) * 100 / 3: 0;
		}

		@Override
		protected String getDescription() {
			return "trigger a pro-" + parameters.nation.get().name + " rebellion in " + parameters.region.get().name;
		}

		@Override
		protected void actualize(World w, Map<String, World.Budget> budgets) {
			w.notifyPlayer(parameters.region.get().getKingdom(), "Plot", "A plot to " + getDescription() + " was successful.");
			parameters.region.get().setKingdom(w, parameters.nation.get().name);
		}
	}
}
