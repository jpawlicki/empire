package com.empire;

final class Relationship {
	War battle;
	Refugees refugees;
	double tribute;
	Construct construct;
	Cede cede;
	Fealty fealty;

	public String diff(Relationship old, String me, String them) {
		String result = "";
		if (!battle.equals(old.battle)) result += " " + battle.summarize(me, them);
		if (!refugees.equals(old.refugees)) result += " " + refugees.summarize(me, them);
		if (!construct.equals(old.construct)) result += " " + construct.summarize(me, them);
		if (tribute != old.tribute) result += " " + me + " will pay " + Math.round(tribute * 100) + "% of their income to " + them;
		return result;
	}

	static enum War {
		ATTACK("%US% will attack %THEM% soldiers on sight."),
		NEUTRAL("%US% will attack %THEM% soldiers only while they trespass in %US%."),
		DEFEND("%US% will never attack %THEM% soldiers.");
		private final String summary;
		private War(String summary) {
			this.summary = summary;
		}
		public String summarize(String us, String them) {
			return summary.replaceAll("%US%", us).replaceAll("%THEM%", them);
		}
	}

	static enum Refugees {
		ACCEPT("%US% will accept %THEM% refugees."),
		REFUSE("%US% will turn away %THEM% refugees.");
		private final String summary;
		private Refugees(String summary) {
			this.summary = summary;
		}
		public String summarize(String us, String them) {
			return summary.replaceAll("%US%", us).replaceAll("%THEM%", them);
		}
	}

	static enum Construct {
		PERMIT("%US% will permit %THEM% to construct within %US%."),
		FORBID("%US% will not permit %THEM% to construct within %US%.");
		private final String summary;
		private Construct(String summary) {
			this.summary = summary;
		}
		public String summarize(String us, String them) {
			return summary.replaceAll("%US%", us).replaceAll("%THEM%", them);
		}
	}

	static enum Cede {
		ACCEPT("%US% will accept regions ceded from %THEM%."),
		REFUSE("%US% will refuse regions ceded from %THEM%.");
		private final String summary;
		private Cede(String summary) {
			this.summary = summary;
		}
		public String summarize(String us, String them) {
			return summary.replaceAll("%US%", us).replaceAll("%THEM%", them);
		}
	}

	static enum Fealty {
		ACCEPT("%US% will accept soldiers transferred from %THEM%."),
		REFUSE("%US% will refuse soldiers transferred from %THEM%.");
		private final String summary;
		private Fealty(String summary) {
			this.summary = summary;
		}
		public String summarize(String us, String them) {
			return summary.replaceAll("%US%", us).replaceAll("%THEM%", them);
		}
	}
}

