package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.model.BotUser;
import telegram.bot.service.enums.BotActionType;
import telegram.bot.service.enums.BotUserPathStage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BotManager {
    private final BotElement botElement;
    private final TelegramBotStorage telegramBotStorage;
    private Map<Long, BotUser> botUsers;

    @PostConstruct
    private void init() {
        botUsers = new HashMap<>();
    }

    synchronized public void handler(Bot bot, Update update) {
        var userId = getUser(update).getId();
        var userName = "@" + getUser(update).getUserName();
        var buttonName = !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getData() : null;

        var botUser = botUsers.putIfAbsent(
                userId,
                BotUser.builder()
                        .botActionType(BotActionType.UNDEFINED)
                        .botUserPathStage(BotUserPathStage.UNDEFINED)
                        .user(telegramBotStorage.getUserByTelegram(userName))
                        .build());

        bot.sendMenu(userId, "Выберите действие", botElement.getMainMenu());
    }

    private User getUser(Update update) {
        return !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
    }
}