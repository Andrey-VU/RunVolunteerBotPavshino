package telegram.bot.adapter.local;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;

@Slf4j
@Component("local")
@RequiredArgsConstructor
public class TelegramBotStorageLocalDBImpl extends Storage implements TelegramBotStorage {
    private final LocalExcelUtils localExcelUtils;

    @PostConstruct
    private void postConstruct() {
        storageUtils = localExcelUtils;
    }
}
