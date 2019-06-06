package com.empire;

import java.util.HashSet;
import java.util.Set;

public class Church {
	public enum Doctrine {
		ANTIAPOSTASY("antiapostasy. Any rulers who have sworn loyalty to the Cult are named apostates and enemies of the Church of Iruhan!", "antiapostacy. The Church follows Iruhan's example of forgiveess by forgiving the sin of aligning with the Cult."),
		ANTIECUMENISM("antiecumenism. Construction of heathen temples or widespread worship of false gods is condemned by the Church!", "antiecumenism. The Church now seeks to spread the Word of Iruhan in the light of tolerance for other faiths."),
		ANTISCHISMATICISM("antischismaticism. The Church declares the Vessel of Faith interpretation of Iruhan's words heretical, and condemns the construction of temples with that emphasis!", "antischismaticism. The Church accepts the right to to study Iruhan's teachings without its direct guidance, albeit unwise."),
		ANTITERRORISM("antiterrorism. The Church reaffirms its condemnations of any attempts to summon the apocalpytic powers that the gothi control.", "antiterrorism. The Church accepts that measured risks when dealing with the Tivar may be necessary to ultimately save lives."),
		CRUSADE("crusade! The Church condemns any Iruhan-worshipping state that fails to declare war on heathen powers!", "crusade. The Church declares that Iruhan's teachings are better followed through peaceful conversion of heathen powers."),
		DEFENDERS_OF_FAITH("recognition of defense of the faith. The Church approves of any who fight to reduce the power of the nations that oppose it.", "recognition of defense of the faith. The Church seeks not to incentivize unnecessary bloodshed in its name."),
		FRATERNITY("fraternity between all Iruhan nations. The Church condemns any Iruhan-worshipping state that makes war against another Iruhan-worshipping state.", "fraternity between Iruhan-worshipping nations. The Church recognizes that tyrants must be removed from power, regardless of the religion of the subjects they oppress."),
		INQUISITION("inquisition! The Church condemns any Iruhan-worshipping state that fails to commit to war against the heretical nations that follow the Vessel of Faith ideology.", "inquisition. The Church announces that the example of Iruhan demonstrates loving tolerance and open discourse."),
		MANDATORY_MINISTRY("mandatory ministry for Cardinals. The Church condemns any ruler willingly keeping his Cardinal out of the Holy City.", "mandatory ministry for Cardinals. The Church acknowledges that its Cardinals can sometimes do more good out in the world, far from the Holy City."),
		WORKS_OF_IRUHAN("recognition of labors to exalt Iruhan. It approves of rulers who construct temples to Iruhan.", "recognition of labors to exalt Iruhan. While the construction of temples is important, rulers cannot buy their way into the Church's favor by stacking up some stones!");

		private final String setString;
		private final String unsetString;

		Doctrine(String setString, String unsetString) {
			this.setString = setString;
			this.unsetString = unsetString;
		}

		public String getSetNotification(String tiecelName, String tiecelKingdom) {
			return tiecelName + ", Tiecel of the Church of Iruhan and Cardinal of " + tiecelKingdom + ", has implemented a doctrine of " + setString;
		}

		public String getUnsetNotification(String tiecelName, String tiecelKingdom) {
			return tiecelName + ", Tiecel of the Church of Iruhan and Cardinal of " + tiecelKingdom + ", has repealed the doctrine of " + unsetString;
		}
	}

	private Set<Doctrine> doctrines = new HashSet<>();

	/** Returns whether the specified policy is currently active. */
	public boolean hasDoctrine(Doctrine p) {
		return doctrines.contains(p);
	}

	public void setDoctrine(Doctrine p) {
		doctrines.add(p);
	}

	public void unsetDoctrine(Doctrine p) {
		doctrines.remove(p);
	}
}
