package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.service.factories.ReplyFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final TelegramBotStorage storage;

    /**
     * Собственно API бота
     */
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    /**
     * Токен телеграма
     */
    @Value("${telegram.token}")
    private String botToken;

    /**
     * Имя бота в телеграме
     */
    @Value("${telegram.bot_name}")
    private String botName;

    /**
     * Список активных регистраций
     */
    private final Map<Long, RegistrationForm> forms = new HashMap<>();

    /**
     * Класс формирующий ответы
     */
    private final ReplyFactory reply = new ReplyFactory();

    @Autowired
    public TelegramBot(TelegramBotStorage storage) throws TelegramApiException {
        log.info("Bot bean created");
        this.storage = storage;
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        log.info("Registerung bot...");
        telegramBotsApi.registerBot(this); // Регистрируем бота
        log.info("Registrartion successfull!!");
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("recieved update!");
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        if(!isKnownUser(userKeys)) {
            if (update.hasMessage() && update.getMessage().getText().equals("/start")) {
                answerToUser(reply.startCommandReply(getChatId(update)));
            }
            answerToUser(reply.registrationRequired(getChatId(update)));
        } else if(update.hasMessage()) {
            if (update.getMessage().getText().startsWith("/")) {
                handleCommand(update);
            } else if (forms.containsKey(userKeys.getKey())) {
                registration(update);
            } else {
                answerToUser(reply.commandNeededMessage(getChatId(update)));
            }
        } else if(update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private void answerToUser(SendMessage message) {
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("Can't send answer! - " + message.toString());
        }
    }

    private Map.Entry<Long, String> getUserKeys(Update update) {
        if (update.hasMessage()) {
            return new AbstractMap.SimpleEntry<Long, String>(
                    update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
        } else if (update.hasCallbackQuery()) {
            return new AbstractMap.SimpleEntry<Long, String>(
                    update.getCallbackQuery().getFrom().getId(), update.getCallbackQuery().getFrom().getUserName());
        }
        throw new RuntimeException("Can't define user");
    }

    private Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        throw new RuntimeException("Can't define chat");
    }

    private boolean isKnownUser(Map.Entry<Long, String> userKeys) {
        return forms.containsKey(userKeys.getKey()) || storage.getUserByTelegram(userKeys.getValue()) != null;
    }

    private void handleCommand(Update update) {

    }

    private void handleCallback(Update update) {

    }

    private void registration(Update update) {

    }

}
