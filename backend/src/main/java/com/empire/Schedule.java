package com.empire;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Schedule {
	List<DayOfWeek> days;
	List<Integer> times; // In minutes after 00:00.
	String locale;

	public long getNextTime() {
		return getNextTimeAfter(Instant.now().atZone(ZoneId.of(locale))).toInstant().toEpochMilli();
	}

	private ZonedDateTime getNextTimeAfter(ZonedDateTime now) {
		ArrayList<ZonedDateTime> candidates = new ArrayList<>();
		for (DayOfWeek d : days) {
			for (Integer time : times) {
				if (now.getDayOfWeek() == d) candidates.add(now.withMinute(0).withHour(0).withSecond(0).plusMinutes(time));
				candidates.add(now.with(TemporalAdjusters.next(d)).withMinute(0).withHour(0).withSecond(0).plusMinutes(time));
			}
		}
		candidates.removeIf(c -> !c.isAfter(now));
		return Collections.min(candidates).toInstant().toEpochMilli();
	}

	public long getNextTimeFirstTurn() {
		return
				getNextTimeAfter(
						getNextTimeAfter(
								Instant.now().atZone(ZoneId.of(locale))))
				.toInstant().toEpochMilli();
	}
}
