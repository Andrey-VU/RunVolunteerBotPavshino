package telegram.bot.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import telegram.bot.service.utils.AESUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Volunteer {
    // Имя
    String name;

    // Фамилия
    String surname;

    // Телеграм пользователя
    String tgUserName;

    // Идентификатор пользователя в системе 5 верст
    String code;

    // Примечание
    String comment;

    // id пользователя в телеграме
    Long tgUserId;

    // является ли пользователь организатором в какую-либо из суббот
    Boolean isOrganizer;

    // подписан ли пользователь на оповещения о записях на роли
    Boolean isSubscribed;

    // номер строки в таблице на закладке "Контакты"
    Integer sheetRowNumber;

    public static Volunteer createFrom(List<String> userProperties, AESUtil aesUtil) {
        var fullNameList = Arrays.asList((!userProperties.isEmpty() ? userProperties : List.of("")).get(0).split(" ", 2));
        return new Volunteer(
                getValueFromList(fullNameList, 0),
                getValueFromList(fullNameList, 1),
                getValueFromList(userProperties, 1),
                getValueFromList(userProperties, 2),
                getValueFromList(userProperties, 3),
                Long.parseLong(aesUtil.decrypt(Optional.ofNullable(getValueFromList(userProperties, 4)).orElse(aesUtil.encrypt("0")))),
                Boolean.parseBoolean(getValueFromList(userProperties, 5)),
                Boolean.parseBoolean(getValueFromList(userProperties, 6)),
                null);
    }

    static String getValueFromList(List<String> list, int elementIndex) {
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

        Volunteer volunteer = (Volunteer) o;

        if (!name.equals(volunteer.name)) return false;
        if (!surname.equals(volunteer.surname)) return false;
        if (!tgUserName.equals(volunteer.tgUserName)) return false;
        if (!code.equals(volunteer.code)) return false;
        return comment.equals(volunteer.comment);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + surname.hashCode();
        result = 31 * result + tgUserName.hashCode();
        result = 31 * result + code.hashCode();
        result = 31 * result + comment.hashCode();
        return result;
    }
}
