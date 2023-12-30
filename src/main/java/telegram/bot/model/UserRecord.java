package telegram.bot.model;

import lombok.Builder;
import lombok.Data;
import telegram.bot.service.enums.UserPathStage;

@Data
@Builder
public class UserRecord {
    private UserPathStage userPathStage;
    private String name;
    private String surname;
    private String code;
}
