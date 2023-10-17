package telegram.bot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.google.TelegramBotStorageGoogleTableImpl;
import telegram.bot.adapter.local.TelegramBotStorageLocalDBImpl;
import telegram.bot.storage.Storage;

@Component("storageChooser")
@RequiredArgsConstructor
@DependsOn({"local", "google"})
public class StorageChooser {
    private final TelegramBotStorageGoogleTableImpl telegramBotStorageGoogleTable;
    private final TelegramBotStorageLocalDBImpl telegramBotStorageLocalDB;

    public Storage getStorage() {
        if (BotConfiguration.getMode() == BotModes.GOOGLE) return telegramBotStorageGoogleTable;
        else if (BotConfiguration.getMode() == BotModes.LOCAL) return telegramBotStorageLocalDB;
        else throw new RuntimeException("error choosing Storage");
    }
}
