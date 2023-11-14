package telegram.bot.model;

import lombok.Builder;
import lombok.Data;
import telegram.bot.service.enums.BotActionType;
import telegram.bot.service.enums.BotActionStage;

@Data
@Builder
public class BotUser {
    private User user;
    private BotActionType botActionType;
    private BotActionStage botActionStage;
}
