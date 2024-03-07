package telegram.bot.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import telegram.bot.service.enums.UserActionType;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRecord {
    UserActionType expectedUserActionType;
    String name;
    String surname;
    String code;
}
