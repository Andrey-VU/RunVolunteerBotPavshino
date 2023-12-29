package telegram.bot.service.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class DatesCalculator {

    private DatesCalculator() {
    }

    public static List<LocalDate> getNextEventDates() {
        LocalDate nearest = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate eventDayTwo = nearest.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate eventDayThree = eventDayTwo.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate eventDayLast = eventDayThree.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        return List.of(nearest, eventDayTwo, eventDayThree, eventDayLast);
    }
}
