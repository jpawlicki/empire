package com.empire;

import java.util.ArrayList;
import java.util.List;

final class Noble {
	String name;
	List<String> tags = new ArrayList<>();
	Crisis crisis;
	double unrest;
}

final class Crisis {
	Type type;
	int deadline;

	static enum Type {
		NONE,
		WEDDING,
		RECESSION,
		BANDITRY,
		BORDER,
		ENNUI,
		CULTISM,
		OVERWHELMED,
		UPRISING,
		STARVATION,
		GUILD
	}
}


