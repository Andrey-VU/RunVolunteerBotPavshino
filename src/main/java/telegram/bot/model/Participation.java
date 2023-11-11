package telegram.bot.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Participation {

    // Пользователь
    private User user;

    // Дата участия
    private LocalDate eventDate;

    // Позиция
    private String eventRole;

    // номер строки в таблице на закладке "Волонтеры"
    private int sheetRowNumber;
}
