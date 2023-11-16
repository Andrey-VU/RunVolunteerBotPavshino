package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.model.Session;
import telegram.bot.service.enums.BotActionStage;
import telegram.bot.service.enums.BotActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SessionManager {
    private final BotElement botElement;
    private final TelegramBotStorage telegramBotStorage;
    private Map<Long, Session> sessions;

    @PostConstruct
    private void init() {
        sessions = new HashMap<>();
    }

    synchronized public void handler(Bot bot, Update update) {
        var userId = getUser(update).getId();
        var userName = "@" + getUser(update).getUserName();
        var userInput = !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getData() : update.getMessage().getText();

        Session session;
        if (sessions.containsKey(userId)) {
            session = sessions.get(userId);
            session.setLastUserInput(userInput);
        } else {
            sessions.put(userId, Session.builder()
                    .user(telegramBotStorage.getUserByTelegram(userName))
                    .botActionStage(BotActionStage.UNDEFINED)
                    .lastUserInput(userInput)
                    .build());
            session = sessions.get(userId);
        }

        proceedBotAction(bot, session);
    }

    private void proceedBotAction(Bot bot, Session session) {
        setNextStage(session);
        switch (session.getBotActionStage()) {
            case PROMPT_FOR_ACTION_TYPE ->
                    bot.sendMenu(session.getUser().getUserId(), BotActionType.getMenuCaption(), botElement.getMainMenu());
            case PROMPT_FOR_SATURDAY -> {
                session.setBotActionType(BotActionType.getBotActionType(session.getLastUserInput()));
                // послать меню суббот
            }
            case PROMPT_FOR_NAME -> {
                // послать промпт для ввода имени
            }
            case PROMPT_FOR_SURNAME -> {
                // послать промпт для ввода фамилии
            }
            case PROMPT_FOR_CODE -> {
                // послать промпт для ввода кода
            }
            case PROMPT_FOR_ROLE -> {
                // созать юзера если его еще нет
                // послать промпт для выбора роли
            }
            case ROLE_CONFIRMATION -> {
                // вывести
            }
            case LIST_BUSY_ROLES -> {
                // послать список ролей
            }
        }
    }

    private void setNextStage(Session session) {
        switch (session.getBotActionStage()) {
            case UNDEFINED, LIST_BUSY_ROLES, ROLE_CONFIRMATION ->
                    session.setBotActionStage(BotActionStage.PROMPT_FOR_ACTION_TYPE);
            case PROMPT_FOR_ACTION_TYPE -> session.setBotActionStage(BotActionStage.PROMPT_FOR_SATURDAY);
            case PROMPT_FOR_SATURDAY -> {
                switch (session.getBotActionType()) {
                    case SAVE ->
                            session.setBotActionStage(Objects.isNull(session.getUser()) ? BotActionStage.PROMPT_FOR_NAME : BotActionStage.PROMPT_FOR_ROLE);
                    case SHOW -> session.setBotActionStage(BotActionStage.LIST_BUSY_ROLES);
                }
            }
            case PROMPT_FOR_NAME -> session.setBotActionStage(BotActionStage.PROMPT_FOR_SURNAME);
            case PROMPT_FOR_SURNAME -> session.setBotActionStage(BotActionStage.PROMPT_FOR_CODE);
            case PROMPT_FOR_CODE -> session.setBotActionStage(BotActionStage.PROMPT_FOR_ROLE);
            case PROMPT_FOR_ROLE -> session.setBotActionStage(BotActionStage.ROLE_CONFIRMATION);
        }
    }

    private User getUser(Update update) {
        return !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
    }
}