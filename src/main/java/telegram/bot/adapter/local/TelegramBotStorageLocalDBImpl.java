package telegram.bot.adapter.local;

import lombok.extern.slf4j.Slf4j;
import telegram.bot.service.utils.AESUtil;
import telegram.bot.storage.Storage;
import telegram.bot.storage.StorageUtils;

@Slf4j
public class TelegramBotStorageLocalDBImpl extends Storage {
    public TelegramBotStorageLocalDBImpl(StorageUtils storageUtils, AESUtil aesUtil) {
        this.storageUtils = storageUtils;
        this.aesUtil = aesUtil;
    }
}
