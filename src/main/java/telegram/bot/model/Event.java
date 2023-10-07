package telegram.bot.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class Event {
    // Дата забега
    private LocalDate eventDate;

    // Команда
    Set<Participation> participants;
}
