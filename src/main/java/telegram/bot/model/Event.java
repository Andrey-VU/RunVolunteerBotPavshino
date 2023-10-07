package telegram.bot.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class Event {
    // Дата забега
    private LocalDate eventDate;

    // Команда
    private List<Participation> participants;

    // номер колонки в sheet
    private int columnNumber;
}
