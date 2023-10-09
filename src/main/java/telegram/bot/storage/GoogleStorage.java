package telegram.bot.storage;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.config.GoogleSheetConfig;
import telegram.bot.model.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component("googleStorage")
public class GoogleStorage extends GenericStorage {
    private GoogleSheetUtils googleSheetUtils;

    @PostConstruct
    private void postConstruct() {
        googleSheetUtils = new GoogleSheetUtils(connectToStorage());
        loadDataFromStorage();
    }

    private Sheets connectToStorage() {
        GoogleCredentials googleCredentials;
        HttpRequestInitializer requestInitializer;
        NetHttpTransport netHttpTransport;

        try {
            googleCredentials = GoogleCredentials.fromStream(new FileInputStream(GoogleSheetConfig.getServiceAccountKeyPath()));
            requestInitializer = new HttpCredentialsAdapter(googleCredentials);
            netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        return new Sheets.Builder(netHttpTransport, GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName(GoogleSheetConfig.getApplicationName())
                .build();
    }

    private void loadDataFromStorage() {
        loadUsers();
        loadEvents();
    }

    private void loadUsers() {
        users = new ArrayList<>();
        List<String> userProperties = new LinkedList<>();
        var rowNumber = GoogleSheetConfig.getSheetContactsStartRow();
        do {
            var rangeBegin = getCellAddress(rowNumber, GoogleSheetConfig.getSheetContactsStartColumnBegin());
            var rangeEnd = getCellAddress(rowNumber++, GoogleSheetConfig.getSheetContactsStartColumnEnd());
            userProperties = googleSheetUtils.readValuesRange(GoogleSheetConfig.getSheetContacts(), rangeBegin, rangeEnd);

            if (!userProperties.get(0).isEmpty()) users.add(new User(userProperties));
            else break;
        } while (true);
    }

    private void loadEvents() {
        var eventColumnBegin = GoogleSheetConfig.getSheetVolunteersEventStartColumn();
        var eventColumnEnd = searchForLastColumnOfFutureSaturdays();
        List<String> roles = getRoles();
    }

    private List<String> getRoles() {
        var roles = new LinkedList<String>();
        var rowNumber = GoogleSheetConfig.getSheetVolunteersRoleStartRow();
        do {
            var cellAddress = getCellAddress(rowNumber++, GoogleSheetConfig.getSheetVolunteersRoleColumn());
            var role = googleSheetUtils.readValueCell(GoogleSheetConfig.getSheetVolunteers(), cellAddress);
            if (!role.isEmpty()) roles.add(role);
            else break;
        } while (true);
        return roles;
    }

    private int searchForLastColumnOfFutureSaturdays() {
        var eventColumn = GoogleSheetConfig.getSheetVolunteersEventStartColumn();
        var saturdaysCounter = 0;
        LocalDate pendingSaturday = null;
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        do {
            var cellAddress = getCellAddress(GoogleSheetConfig.getSheetVolunteersEventDateRow(), eventColumn++);
            var dateString = googleSheetUtils.readValueCell(GoogleSheetConfig.getSheetVolunteers(), cellAddress);
            if (dateString.isEmpty()) {
                pendingSaturday = getNextSaturday(Optional.ofNullable(pendingSaturday).orElse(LocalDate.now()));
                googleSheetUtils.writeCellValue(GoogleSheetConfig.getSheetVolunteers(), cellAddress, pendingSaturday.format(dateFormat));
                saturdaysCounter++;
            } else {
                pendingSaturday = LocalDate.parse(dateString, dateFormat);
                if (pendingSaturday.isAfter(LocalDate.now())) saturdaysCounter++;
            }
        } while (saturdaysCounter < GoogleSheetConfig.getSheetSaturdaysAhead());
        return --eventColumn;
    }

    private LocalDate getNextSaturday(LocalDate day) {
        do day = day.plusDays(1);
        while (day.getDayOfWeek() != DayOfWeek.SATURDAY);
        return day;
    }

    private String getCellAddress(int rowNumber, int columnNumber) {
        var rowPrefix = "R";
        var columnPrefix = "C";
        var rowAddress = rowPrefix + rowNumber;
        var columnAddress = columnPrefix + columnNumber;
        return rowAddress + columnAddress;
    }
}
