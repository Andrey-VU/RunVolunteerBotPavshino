package telegram.bot.model;

import lombok.Builder;
import lombok.Data;
import telegram.bot.service.enums.UserActionType;

@Data
@Builder
public class UserRecord {
    private UserActionType expectedUserActionType;
    private String name;
    private String surname;
    private String code;
}
