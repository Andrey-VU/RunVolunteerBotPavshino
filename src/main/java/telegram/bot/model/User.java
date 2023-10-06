package telegram.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {

    // Имя
    private String name;

    // Фамилия
    private String surname;

    // Телеграм пользоваителя
    private String telegram;

    // Идентификатор пользователя в системе 5 верст
    private String code;
}
