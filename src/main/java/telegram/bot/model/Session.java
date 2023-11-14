package telegram.bot.model;

import lombok.Builder;
import lombok.Data;
import telegram.bot.service.enums.BotActionStage;
import telegram.bot.service.enums.BotActionType;

@Data
@Builder
public class Session {
    private User user;
    private BotActionType botActionType;
    private BotActionStage botActionStage;
    private String lastUserInput;
}
