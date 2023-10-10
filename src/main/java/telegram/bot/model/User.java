package telegram.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
public class User {
    // Имя
    private String name;

    // Фамилия
    private String surname;

    // Телеграм пользователя
    private String telegram;

    // Идентификатор пользователя в системе 5 верст
    private String code;

    public static User createFrom(List<String> userProperties) {
        var fullNameList = Arrays.asList((!userProperties.isEmpty() ? userProperties : List.of("")).get(0).split(" ", 2));
        return new User(
                getValueFromList(fullNameList, 0),
                getValueFromList(fullNameList, 1),
                getValueFromList(userProperties, 1),
                getValueFromList(userProperties, 2));
    }

    private static String getValueFromList(List<String> list, int elementIndex) {
        return !Objects.isNull(list) && list.size() >= elementIndex + 1 && !list.get(elementIndex).isEmpty() ? list.get(elementIndex) : null;
    }

    public String getFullName() {
        return name + " " + surname;
    }
}
