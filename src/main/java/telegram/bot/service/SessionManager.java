package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
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

        var session = sessions.putIfAbsent(
                userId,
                Session.builder()
                        .bot(bot)
                        .user(telegramBotStorage.getUserByTelegram(userName))
                        .botActionType(BotActionType.UNDEFINED)
                        .botActionStage(BotActionStage.UNDEFINED)
                        .build());

        assert session != null;
        chooseNextStage(session);
        proceedBotAction(session);
    }

    private void chooseNextStage(Session session) {
        switch (session.getBotActionStage()) {
            case UNDEFINED, LIST_BUSY_ROLES, PROMPT_FOR_ROLE ->
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
        }
    }

    private void proceedBotAction(Session session) {
        switch (session.getBotActionStage()) {
            case PROMPT_FOR_ACTION_TYPE -> session.setBotActionType(
                    BotActionType.getBotActionType(
                            getUserPressedButton(
                                    session,
                                    BotActionType.getMenuCaption(),
                                    botElement.getMainMenu())));
            case PROMPT_FOR_SATURDAY -> {
            }
            case PROMPT_FOR_NAME -> {
            }
            case PROMPT_FOR_SURNAME -> {
            }
            case PROMPT_FOR_CODE -> {
            }
            case PROMPT_FOR_ROLE -> {
            }
        }
    }

    private String getUserPressedButton(Session session, String prompt, ReplyKeyboard replyKeyboard) {
        session.getBot().sendMenu(session.getUser().getUserId(), prompt, replyKeyboard);
        return null;
    }

    private String getUserEnteredString() {
        return null;
    }

    private User getUser(Update update) {
        return !Objects.isNull(update.getCallbackQuery()) ? update.getCallbackQuery().getFrom() : update.getMessage().getFrom();
    }
}