package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.config.BotConfiguration;

@Slf4j
@Component
public class Bot extends TelegramLongPollingBot {
    private final BotManager botManager;

    @Autowired
    public Bot(BotManager botManager) {
        super(BotConfiguration.getTelegramBotToken());
        this.botManager = botManager;
    }

    @PostConstruct
    private void postConstruct() throws TelegramApiException {
        new TelegramBotsApi(DefaultBotSession.class).registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return BotConfiguration.getTelegramBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        botManager.handler(this, update);
    }

    public void sendMenu(Long userId, String caption, ReplyKeyboard kb) {
        SendMessage message = SendMessage.builder()
                .chatId(userId.toString())
                .parseMode("HTML")
                .text(caption)
                .replyMarkup(kb)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendText(Long userId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(userId.toString())
                .text(text).build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
