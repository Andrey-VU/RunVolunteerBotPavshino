package telegram.bot.adapter.google;

import lombok.extern.slf4j.Slf4j;
import telegram.bot.storage.GoogleSheetUtils;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;

@Slf4j
public class TelegramBotStorageGoogleTableImpl extends Storage {
    private final LocalExcelUtils localExcelUtils;

    public TelegramBotStorageGoogleTableImpl(GoogleSheetUtils googleSheetUtils, LocalExcelUtils localExcelUtils) {
        this.storageUtils = googleSheetUtils;
        this.localExcelUtils = localExcelUtils;
    }

    @Override
    public void loadDataFromStorage() {
        super.loadDataFromStorage();
        localExcelUtils.initExcelFile(contacts, events);
    }
}
