package com.empire;

import java.util.Objects;

public class Nation {
	public String rulerName = "";
	public String title = "";
	public String food = "";
	public String happiness = "";
	public String territory = "";
	public String glory = "";
	public String religion = "";
	public String ideology = "";
	public String security = "";
	public String riches = "";
	public String culture = "";
	public NationData.Tag trait1;
	public NationData.Tag trait2;
	public Ideology dominantIdeology;
	public String bonus = "";
	public String email = "";
	public String password = "";

	public boolean hasTag(NationData.Tag tag) {
		return trait1 == tag || trait2 == tag;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Nation nation = (Nation) o;
		return Objects.equals(rulerName, nation.rulerName) &&
				Objects.equals(title, nation.title) &&
				Objects.equals(food, nation.food) &&
				Objects.equals(happiness, nation.happiness) &&
				Objects.equals(territory, nation.territory) &&
				Objects.equals(glory, nation.glory) &&
				Objects.equals(religion, nation.religion) &&
				Objects.equals(ideology, nation.ideology) &&
				Objects.equals(security, nation.security) &&
				Objects.equals(riches, nation.riches) &&
				Objects.equals(culture, nation.culture) &&
				trait1 == nation.trait1 &&
				trait2 == nation.trait2 &&
				dominantIdeology == nation.dominantIdeology &&
				Objects.equals(bonus, nation.bonus) &&
				Objects.equals(email, nation.email) &&
				Objects.equals(password, nation.password);
	}

	@Override
	public int hashCode() {
		return Objects.hash(rulerName, title, food, happiness, territory, glory, religion, ideology, security, riches, culture, trait1, trait2, dominantIdeology, bonus, email, password);
	}
}
