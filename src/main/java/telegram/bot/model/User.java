package telegram.bot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class User {

    // Имя
    private String name;

    // Фамилия
    private String surname;

    // Телеграм пользователя
    private String telegram;

    // Идентификатор пользователя в системе 5 верст
    private String code;

    public User(List<String> userProperties) {
        name = userProperties.get(0).split(" ")[0];
        surname = userProperties.get(0).split(" ")[1];
        telegram = userProperties.size() >= 2 ? userProperties.get(1) : null;
        code = userProperties.size() >= 3 ? userProperties.get(2) : null;
    }
}
