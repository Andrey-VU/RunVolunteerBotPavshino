package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.adapter.TelegramBotStorage;

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

    @Autowired
    public TelegramBot(TelegramBotStorage storage) throws TelegramApiException {
        this.storage = storage;
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        // telegramBotsApi.registerBot(this); // Регистрируем бота
    }

    @Override
    public void onUpdateReceived(Update update) {
        //Проверим, работает ли наш бот.
        System.out.println(update.getMessage().getText());
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
