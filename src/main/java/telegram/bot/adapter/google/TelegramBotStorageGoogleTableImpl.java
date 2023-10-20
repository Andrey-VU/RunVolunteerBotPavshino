package telegram.bot.adapter.google;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.config.BotModes;
import telegram.bot.storage.GoogleSheetUtils;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;

@Slf4j
@Component("google")
@RequiredArgsConstructor
public class TelegramBotStorageGoogleTableImpl extends Storage {
    private final GoogleSheetUtils googleSheetUtils;
    private final LocalExcelUtils localExcelUtils;

    @PostConstruct
    private void postConstruct() {
        storageUtils = googleSheetUtils;
        if (BotConfiguration.getMode() == BotModes.GOOGLE) loadDataFromStorage();
    }

    @Override
    protected void loadDataFromStorage() {
        super.loadDataFromStorage();
        localExcelUtils.initExcelFile(contacts, events);


    }
}
