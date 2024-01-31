package telegram.bot.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level= AccessLevel.PRIVATE)
public class Participation {

    // Волонтер
    Volunteer volunteer;

    // Дата участия
    LocalDate eventDate;

    // Позиция
    String eventRole;

    // номер строки в таблице на закладке "Волонтеры"
    int sheetRowNumber;

    // используется в UI бота, чтобы пометить кнопку, с помощью которой вызывается 2 часть списка доступных ролей при записи волонтера
    boolean pointerToNextPageOfRoles;
}
