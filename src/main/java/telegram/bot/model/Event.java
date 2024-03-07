package telegram.bot.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    // Дата забега
    LocalDate eventDate;

    // Команда
    List<Participation> participants;

    // номер колонки в sheet
    int columnNumber;

    public static String getDateLocalized(LocalDate date) {
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).localizedBy(new Locale("ru")));
    }
}
