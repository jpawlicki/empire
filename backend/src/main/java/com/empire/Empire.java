package com.empire;

public class Empire {
	public static Kingdom[] kingdoms = {
		new Kingdom("Dumm", "", ""),
		new Kingdom("Ejymsyn", "Alex Pawlicki", "alexanderpawlicki@gmail.com"),
		new Kingdom("Fulyn", "", ""),
		new Kingdom("Mabe", "Mark Cignarale", "markcig.bww@gmail.com"),
		new Kingdom("Miycc", "Ben McRae", "bbswiatek@gmail.com"),
		new Kingdom("Muoem", "Robin Hill", "rehill012@gmail.com"),
		new Kingdom("Neamx", "Alex Lombardo", "tochimo@gmail.com"),
		new Kingdom("Siylmae", "Edith Hanson", "edithruthhanson@gmail.com"),
		new Kingdom("Slail", "Aaron Cignarale", "cigna88@gmail.com"),
		new Kingdom("Telmymsyn", "Alex Childs", "achilds@lbostore.com"),
		new Kingdom("Turujmi", "Joey Griggs", "jgriggs@lbostore.com"),
		new Kingdom("Walymyn", "Mike Luk", "mluk@sflscientific.com"),
		new Kingdom("Zemsim", "", ""),
		new Kingdom("Aefoss", "Mike Pawlicki", "mnpaws@gmail.com"),
		new Kingdom("Doomstoll", "", ""),
		new Kingdom("Fiskrbaer", "Ashlee Daniel", "ashlee.s.daniel@gmail.com"),
		new Kingdom("Holmslatr", "Peter Pawlicki", "ptpawlicki@gmail.com"),
		new Kingdom("Hosshofn", "Alex Holland", "quallenhk@gmail.com"),
		new Kingdom("Hrip", "Kyle", "shaggyhaggiers@gmail.com"),
		new Kingdom("Katanes", "James Hanson", "james7hanson@gmail.com"),
		new Kingdom("Mestrhofn", "Michael Engard", "kaelri@gmail.com"),
		new Kingdom("Fiskrfjord", "Jesse Cruz", "jcruz588@yahoo.com"),
		new Kingdom("Grind", "Ryan Kleinschmidt", "r.kleinschmidt@yahoo.com"),
		new Kingdom("Vatnheim", "Warren McGrail", "bingelljk@gmail.com"),
		new Kingdom("Verbjalnic", "", ""),
		new Kingdom("Order", "", ""),
		new Kingdom("Church%20of%20Iruhan", "", "")
	};

	public static class Kingdom {
		public final String name;
		public final String playerName;
		public final String email;

		public Kingdom(String name, String playerName, String email) {
			this.name = name;
			this.playerName = playerName;
			this.email = email;
		}
	}
}
