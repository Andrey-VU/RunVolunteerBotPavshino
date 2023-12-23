package telegram.bot.adapter.google;

import lombok.extern.slf4j.Slf4j;
import telegram.bot.service.AESUtil;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;
import telegram.bot.storage.google.GoogleSheetUtils;

@Slf4j
public class TelegramBotStorageGoogleTableImpl extends Storage {
    private final LocalExcelUtils localExcelUtils;

    public TelegramBotStorageGoogleTableImpl(GoogleSheetUtils googleSheetUtils, LocalExcelUtils localExcelUtils, AESUtil aesUtil) {
        this.storageUtils = googleSheetUtils;
        this.localExcelUtils = localExcelUtils;
        this.aesUtil = aesUtil;
    }

    @Override
    public void loadDataFromStorage() {
        super.loadDataFromStorage();
        localExcelUtils.initExcelFile(contacts, events);
    }
}
