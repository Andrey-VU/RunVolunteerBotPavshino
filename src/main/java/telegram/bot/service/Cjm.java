package telegram.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import telegram.bot.adapter.TelegramBotStorage;

@Component
@RequiredArgsConstructor
public class Cjm {
    private final TelegramBotStorage telegramBotStorage;

    public void handler(Bot bot, Update update) {
    }
}
