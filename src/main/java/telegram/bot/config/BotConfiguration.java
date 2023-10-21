package telegram.bot.config;


import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.adapter.google.TelegramBotStorageGoogleTableImpl;
import telegram.bot.adapter.local.TelegramBotStorageLocalDBImpl;
import telegram.bot.storage.GoogleSheetUtils;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/bot-config.properties", ignoreResourceNotFound = false)
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/googleSheet.properties", ignoreResourceNotFound = false, encoding = "UTF-8")
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/localExcel.properties", ignoreResourceNotFound = false, encoding = "UTF-8")
public class BotConfiguration {
    /**
     * Режим бота
     */
    @Getter
    @Setter
    private static BotModes mode;

    private static String GOOGLE_APPLICATION_NAME;
    private static String GOOGLE_SERVICE_ACCOUNT_KEY_PATH;
    private static long GOOGLE_API_PAUSE_LONG;
    private static String GOOGLE_SHEET_ID;
    private static String SHEET_VOLUNTEERS;
    private static String SHEET_CONTACTS;
    private static int SHEET_CONTACTS_ROW_START;
    private static int SHEET_CONTACTS_COLUMN_FIRST;
    private static int SHEET_CONTACTS_COLUMN_LAST;
    private static int SHEET_VOLUNTEERS_ROLE_ROW_START;
    private static int SHEET_VOLUNTEERS_ROLE_COLUMN;
    private static int SHEET_VOLUNTEERS_EVENT_COLUMN_START;
    private static int SHEET_VOLUNTEERS_EVENT_ROW;
    private static int SHEET_SATURDAYS_AHEAD;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Value("${local.storage.path}")
    String LOCAL_STORAGE_PATH;

    @Bean
    public TelegramBotStorage getTelegramBotStorage() {
        Storage telegramBotStorage;

        if (BotConfiguration.getMode() == BotModes.GOOGLE)
            telegramBotStorage = new TelegramBotStorageGoogleTableImpl(new GoogleSheetUtils(connectionToGoogleStorage()), new LocalExcelUtils(LOCAL_STORAGE_PATH));
        else if (BotConfiguration.getMode() == BotModes.LOCAL)
            telegramBotStorage = new TelegramBotStorageLocalDBImpl(new LocalExcelUtils(LOCAL_STORAGE_PATH));
        else throw new RuntimeException("error choosing Storage");

        telegramBotStorage.loadDataFromStorage();

        return telegramBotStorage;
    }

    public BotConfiguration(@Value("${google.app.name}") String google_app_name,
                            @Value("${google.service.account.key}") String google_service_account_key,
                            @Value("${google.api.pause.ms}") String google_api_pause_ms,
                            @Value("${google.sheet.id}") String google_sheet_id,
                            @Value("${sheet.volunteers}") String sheet_volunteers,
                            @Value("${sheet.contacts}") String sheet_contacts,
                            @Value("${sheet.contacts.row.start}") String sheet_contacts_row_start,
                            @Value("${sheet.contacts.column.first}") String sheet_contacts_column_first,
                            @Value("${sheet.contacts.column.last}") String sheet_contacts_column_last,
                            @Value("${sheet.volunteers.role.row.start}") String sheet_volunteers_role_row_start,
                            @Value("${sheet.volunteers.role.column}") String sheet_volunteers_role_column,
                            @Value("${sheet.volunteers.event.column.start}") String sheet_volunteers_event_column_start,
                            @Value("${sheet.volunteers.event.row}") String sheet_volunteers_event_row,
                            @Value("${sheet.saturdays.ahead}") String sheet_saturdays_ahead) {
        GOOGLE_APPLICATION_NAME = google_app_name;
        GOOGLE_SERVICE_ACCOUNT_KEY_PATH = google_service_account_key;
        GOOGLE_API_PAUSE_LONG = Long.parseLong(google_api_pause_ms);
        GOOGLE_SHEET_ID = google_sheet_id;
        SHEET_VOLUNTEERS = sheet_volunteers;
        SHEET_CONTACTS = sheet_contacts;
        SHEET_CONTACTS_ROW_START = Integer.parseInt(sheet_contacts_row_start);
        SHEET_CONTACTS_COLUMN_FIRST = Integer.parseInt(sheet_contacts_column_first);
        SHEET_CONTACTS_COLUMN_LAST = Integer.parseInt(sheet_contacts_column_last);
        SHEET_VOLUNTEERS_ROLE_ROW_START = Integer.parseInt(sheet_volunteers_role_row_start);
        SHEET_VOLUNTEERS_ROLE_COLUMN = Integer.parseInt(sheet_volunteers_role_column);
        SHEET_VOLUNTEERS_EVENT_COLUMN_START = Integer.parseInt(sheet_volunteers_event_column_start);
        SHEET_VOLUNTEERS_EVENT_ROW = Integer.parseInt(sheet_volunteers_event_row);
        SHEET_SATURDAYS_AHEAD = Integer.parseInt(sheet_saturdays_ahead);
    }

    public static String getGoogleApplicationName() {
        return GOOGLE_APPLICATION_NAME;
    }

    public static String getGoogleServiceAccountKeyPath() {
        return GOOGLE_SERVICE_ACCOUNT_KEY_PATH;
    }

    public static long getGoogleApiPauseLong() {
        return GOOGLE_API_PAUSE_LONG;
    }

    public static String getGoogleSheetId() {
        return GOOGLE_SHEET_ID;
    }

    public static String getSheetVolunteers() {
        return SHEET_VOLUNTEERS;
    }

    public static String getSheetContacts() {
        return SHEET_CONTACTS;
    }

    public static int getSheetContactsRowStart() {
        return SHEET_CONTACTS_ROW_START;
    }

    public static int getSheetContactsColumnFirst() {
        return SHEET_CONTACTS_COLUMN_FIRST;
    }

    public static int getSheetContactsColumnLast() {
        return SHEET_CONTACTS_COLUMN_LAST;
    }

    public static int getSheetVolunteersRoleRowStart() {
        return SHEET_VOLUNTEERS_ROLE_ROW_START;
    }

    public static int getSheetVolunteersRoleColumn() {
        return SHEET_VOLUNTEERS_ROLE_COLUMN;
    }

    public static int getSheetVolunteersEventColumnStart() {
        return SHEET_VOLUNTEERS_EVENT_COLUMN_START;
    }

    public static int getSheetVolunteersEventRow() {
        return SHEET_VOLUNTEERS_EVENT_ROW;
    }

    public static int getSheetSaturdaysAhead() {
        return SHEET_SATURDAYS_AHEAD;
    }

    private Sheets connectionToGoogleStorage() {
        GoogleCredentials googleCredentials;
        HttpRequestInitializer requestInitializer;
        NetHttpTransport netHttpTransport;

        try {
            System.out.println(BotConfiguration.getGoogleServiceAccountKeyPath());
            googleCredentials = GoogleCredentials.fromStream(new FileInputStream(BotConfiguration.getGoogleServiceAccountKeyPath()));
            requestInitializer = new HttpCredentialsAdapter(googleCredentials);
            netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        return new Sheets.Builder(netHttpTransport, GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName(BotConfiguration.getGoogleApplicationName())
                .build();
    }
}
