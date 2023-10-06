package telegram.bot.model;

import lombok.Data;

@Data
public class User {

    // Имя
    public String name;

    // Фамилия
    public String surname;

    // Телеграм пользоваителя
    public String telegram;

    // Идентификатор пользователя в системе 5 верст
    public String code;
}
