package telegram.bot.adapter.local;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.model.Participation;
import telegram.bot.model.User;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("local")
@RequiredArgsConstructor
public class TelegramBotStorageLocalDBImpl extends Storage implements TelegramBotStorage {
    private final LocalExcelUtils localExcelUtils;

    @Override
    public User saveUser(User user) {
        return null;
    }

    @Override
    public User getUserByTelegram(String telegram) {
        return null;
    }

    @Override
    public User getUserByCode(String code) {
        return null;
    }

    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public List<Participation> getParticipantsByDate(LocalDate date) {
        return null;
    }

    @Override
    public List<Participation> getAvailableParticipationByDate(LocalDate date) {
        return null;
    }

    @Override
    public Participation saveParticipation(Participation participation) {
        return null;
    }

    @Override
    public void deleteParticipation(Participation participation) {
    }
    @PostConstruct
    private void postConstruct() throws IOException {
        loadDataFromLocalExcelFile();
    }

    private void loadDataFromLocalExcelFile() throws IOException {
        loadContacts();
        loadEvents();
    }

    private void loadEvents() throws IOException {

    }

    private void loadContacts() throws IOException {
        Map<Integer, List<String>> contactsFromExcel = localExcelUtils.readXLSXFile(0);
    }

}
