package com.empire;

class RulesObject {
	private transient final Rules rules;

	protected RulesObject(Rules rules) {
		this.rules = rules;
	}

	protected Rules getRules() { return rules; }
}
