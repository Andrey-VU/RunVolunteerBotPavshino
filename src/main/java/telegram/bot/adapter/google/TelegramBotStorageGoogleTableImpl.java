package telegram.bot.adapter.google;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.model.Participation;
import telegram.bot.storage.GoogleSheetUtils;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;

@Slf4j
@Component("google")
@RequiredArgsConstructor
public class TelegramBotStorageGoogleTableImpl extends Storage implements TelegramBotStorage {
    private final GoogleSheetUtils googleSheetUtils;
    private final LocalExcelUtils localExcelUtils;

    @Override
    public Participation saveParticipation(Participation participation) {
        loadDataFromGoogleSheets();
        return super.saveParticipation(participation);
    }

    @PostConstruct
    private void postConstruct() {
        storageUtils = googleSheetUtils;
        loadDataFromGoogleSheets();
    }

    private void loadDataFromGoogleSheets() {
        loadContacts();
        loadEvents();
        localExcelUtils.writeContactsToExcel(contacts);
        localExcelUtils.writeVolunteersToExcel(events);
    }
}
