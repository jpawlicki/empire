package com.empire;

enum Season {
	AUTUMN(3),
	WINTER(2),
	SPRING(4),
	SUMMER(7);

	static Season get(int date) {
		date = date % 48;
		if (date >= 44) return AUTUMN;
		if (date >= 32) return SUMMER;
		if (date >= 20) return SPRING;
		if (date >=  8) return WINTER;
		return AUTUMN;
	}

	static boolean isHarvest(int date) {
		return date % 4 == 0;
	}

	private final double crops;

	double getCrops() {
		return crops;
	}

	private Season(double crops) {
		this.crops = crops;
	}
}
