package telegram.bot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        var fullName = Arrays.asList((!userProperties.isEmpty() ? userProperties : List.of("")).get(0).split(" ", 2));
        name = getValueFromList(fullName, 0);
        surname = getValueFromList(fullName, 1);
        telegram = getValueFromList(userProperties, 1);
        code = getValueFromList(userProperties, 2);
    }

    private String getValueFromList(List<String> list, int elementIndex) {
        return !Objects.isNull(list) && list.size() >= elementIndex + 1 && !list.get(elementIndex).isEmpty() ? list.get(elementIndex) : null;
    }

    public String getFullName() {
        return name + " " + surname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!Objects.equals(name, user.name)) return false;
        return Objects.equals(surname, user.surname);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (surname != null ? surname.hashCode() : 0);
        return result;
    }
}
