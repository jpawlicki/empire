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
	int time; // In minutes after 00:00.
	String locale;

	public long getNextTime() {
		ZonedDateTime now = Instant.now().atZone(ZoneId.of(locale));
		ArrayList<ZonedDateTime> candidates = new ArrayList<>();
		for (DayOfWeek d : days) candidates.add(now.with(TemporalAdjusters.next(d)).withMinute(0).withHour(0).withSecond(0).plusMinutes(time));
		return Collections.min(candidates).toInstant().toEpochMilli();
	}
}
