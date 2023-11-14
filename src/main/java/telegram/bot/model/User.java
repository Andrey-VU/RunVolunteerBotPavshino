package telegram.bot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
public class User {
    // Имя
    private String name;

    // Фамилия
    private String surname;

    // Телеграм пользователя (@userName)
    private String userName;

    // Телеграм пользователя (userId)
    private String userId;

    // Идентификатор пользователя в системе 5 верст
    private String code;

    // Примечание
    private String comment;

    public static User createFrom(String name, String surname, String userName, String code) {
        return new User(name, surname, userName, null, code, null);
    }

    public static User createFrom(List<String> userProperties) {
        var fullNameList = Arrays.asList((!userProperties.isEmpty() ? userProperties : List.of("")).get(0).split(" ", 2));
        return new User(
                getValueFromList(fullNameList, 0),
                getValueFromList(fullNameList, 1),
                getValueFromList(userProperties, 1),
                null,
                getValueFromList(userProperties, 2),
                getValueFromList(userProperties, 3));
    }

    private static String getValueFromList(List<String> list, int elementIndex) {
        return !Objects.isNull(list) && list.size() >= elementIndex + 1 && !list.get(elementIndex).isEmpty() ? list.get(elementIndex) : null;
    }

    public String getFullName() {
        var fullName = Optional.ofNullable(name).orElse("") + " " + Optional.ofNullable(surname).orElse("");
        return fullName.isBlank() ? "" : fullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!name.equals(user.name)) return false;
        if (!surname.equals(user.surname)) return false;
        if (!userName.equals(user.userName)) return false;
        if (!code.equals(user.code)) return false;
        return comment.equals(user.comment);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + surname.hashCode();
        result = 31 * result + userName.hashCode();
        result = 31 * result + code.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }
}
