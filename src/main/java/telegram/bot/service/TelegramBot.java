package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    // Создаём их объект для регистрации
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    // Достаём токен бота
    @Value("${telegram.token}")
    private String botToken;

    public TelegramBot() throws TelegramApiException {
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        telegramBotsApi.registerBot(this); // Регистрируем бота
    }

    @Override
    public void onUpdateReceived(Update update) {
        //Проверим, работает ли наш бот.
        System.out.println(update.getMessage().getText());
    }

    @Override
    public String getBotUsername() {
        return "bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
