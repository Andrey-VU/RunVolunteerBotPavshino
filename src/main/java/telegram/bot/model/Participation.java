package telegram.bot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class Participation {

    // Пользователь
    private User user;

    // Дата участия
    private LocalDate eventDate;
}
