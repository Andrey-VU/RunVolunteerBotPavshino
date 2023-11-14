package telegram.bot.model;

import lombok.Builder;
import lombok.Getter;
import telegram.bot.service.Bot;

@Getter
@Builder
public class BotAction {
    private final BotUser botUser;
    private final String botActionResult;
}
