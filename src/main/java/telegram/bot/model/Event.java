package telegram.bot.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

@Data
@Builder
public class Event {
    // Дата забега
    private LocalDate eventDate;

    // Команда
    private List<Participation> participants;

    // номер колонки в sheet
    private int columnNumber;

    public static String getDateLocalized(LocalDate date) {
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).localizedBy(new Locale("ru")));
    }
}
