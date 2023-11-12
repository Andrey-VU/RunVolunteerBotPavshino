package telegram.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import telegram.bot.adapter.TelegramBotStorage;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class Cjm {
    private final Elements elements;
    private final TelegramBotStorage telegramBotStorage;

    public void handler(Bot bot, Update update) {
        var userId = !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getFrom().getId() : update.getMessage().getFrom().getId();
        var buttonName = !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getData() : null;

//        if (!Objects.isNull(buttonName))
//            bot.sendText(userId, buttonName);

        bot.sendMenu(userId, "Выберите действие", elements.getMainMenu());
    }
}