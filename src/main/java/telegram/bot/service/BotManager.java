package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.model.BotAction;
import telegram.bot.model.BotUser;
import telegram.bot.service.enums.BotActionStage;
import telegram.bot.service.enums.BotActionType;

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
        var userFeedback = !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getData() : null;

        var botUser = botUsers.putIfAbsent(
                userId,
                BotUser.builder()
                        .bot(bot)
                        .user(telegramBotStorage.getUserByTelegram(userName))
                        .botActionType(BotActionType.UNDEFINED)
                        .botActionStage(BotActionStage.UNDEFINED)
                        .build());

        assert botUser != null;
        chooseNextStage(botUser);
        proceedBotAction(BotAction.builder().botUser(botUser).build());
    }

    private void chooseNextStage(BotUser botUser) {
        switch (botUser.getBotActionStage()) {
            case UNDEFINED, LIST_ROLES_FOR_SATURDAY, PROMPT_FOR_ROLE ->
                    botUser.setBotActionStage(BotActionStage.PROMPT_FOR_ACTION_TYPE);
            case PROMPT_FOR_ACTION_TYPE -> botUser.setBotActionStage(BotActionStage.PROMPT_FOR_SATURDAY);
            case PROMPT_FOR_SATURDAY -> {
                switch (botUser.getBotActionType()) {
                    case SAVE ->
                            botUser.setBotActionStage(!Objects.isNull(botUser.getUser()) ? BotActionStage.PROMPT_FOR_ROLE : BotActionStage.PROMPT_FOR_NAME);
                    case SHOW -> botUser.setBotActionStage(BotActionStage.LIST_ROLES_FOR_SATURDAY);
                }
            }
            case PROMPT_FOR_NAME -> botUser.setBotActionStage(BotActionStage.PROMPT_FOR_SURNAME);
            case PROMPT_FOR_SURNAME -> botUser.setBotActionStage(BotActionStage.PROMPT_FOR_CODE);
            case PROMPT_FOR_CODE -> botUser.setBotActionStage(BotActionStage.PROMPT_FOR_ROLE);
        }
    }

    private void proceedBotAction(BotAction botAction) {
        //botAction.getBot().sendMenu(userId, BotActionType.SHOW.getMenuCaption(), botElement.getMainMenu());
    }

    private User getUser(Update update) {
        return !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
    }
}