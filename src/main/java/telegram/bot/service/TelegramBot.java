package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.config.BotModes;
import telegram.bot.config.StorageChooser;

@Component
@DependsOn("storageChooser")
public class TelegramBot extends TelegramLongPollingBot {

    private final static BotModes mode = BotConfiguration.getMode();

    /**
     * Собственно API бота
     */
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    private final TelegramBotStorage storage;

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
    public TelegramBot(StorageChooser storageChooser) throws TelegramApiException {
        this.storage = storageChooser.getStorage();
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
