package telegram.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.adapter.google.TelegramBotStorageGoogleTableImpl;
import telegram.bot.adapter.local.TelegramBotStorageLocalDBImpl;
import telegram.bot.service.Bot;
import telegram.bot.storage.LocalExcelUtils;
import telegram.bot.storage.Storage;
import telegram.bot.storage.google.GoogleConnection;
import telegram.bot.storage.google.GoogleSheetUtils;

import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
@PropertySource(value = "file:${RunVolunteerBotPavshinoLocalConfigDir}/bot-config.properties", ignoreResourceNotFound = false)
@PropertySource(value = "file:${RunVolunteerBotPavshinoLocalConfigDir}/sheet.properties", ignoreResourceNotFound = false, encoding = "UTF-8")
public class BotConfiguration {
    /**
     * Режим бота
     */
    private static String TELEGRAM_BOT_TOKEN;
    private static String TELEGRAM_BOT_NAME;
    private static BotStorageMode BOT_STORAGE_MODE;
    private static long BOT_STORAGE_SHEET_SYNC_INTERVAL_ms;
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

    @Value("${local.excel.file.path}")
    private String LOCAL_EXCEL_FILE_PATH;

    @Bean
    public TelegramBotStorage getTelegramBotStorage() {
        Storage telegramBotStorage;

        if (BOT_STORAGE_MODE == BotStorageMode.GOOGLE)
            telegramBotStorage = new TelegramBotStorageGoogleTableImpl(new GoogleSheetUtils(new GoogleConnection()), new LocalExcelUtils(LOCAL_EXCEL_FILE_PATH));
        else if (BOT_STORAGE_MODE == BotStorageMode.LOCAL)
            telegramBotStorage = new TelegramBotStorageLocalDBImpl(new LocalExcelUtils(LOCAL_EXCEL_FILE_PATH));
        else throw new RuntimeException("error choosing Storage");

        telegramBotStorage.loadDataFromStorage();

        return telegramBotStorage;
    }

    public BotConfiguration(@Value("${telegram.bot.token}") String telegram_bot_token,
                            @Value("${telegram.bot.name}") String telegram_bot_name,
                            @Value("${bot.storage.mode}") String bot_storage_mode,
                            @Value("${bot.storage.sheet.sync.interval.sec}") String bot_storage_sheet_sync_interval_sec,
                            @Value("${google.app.name}") String google_app_name,
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

        TELEGRAM_BOT_TOKEN = telegram_bot_token;
        TELEGRAM_BOT_NAME = telegram_bot_name;

        if (bot_storage_mode.equals(BotStorageMode.GOOGLE.toString()))
            BotConfiguration.BOT_STORAGE_MODE = BotStorageMode.GOOGLE;
        else if (bot_storage_mode.equals(BotStorageMode.LOCAL.toString()))
            BotConfiguration.BOT_STORAGE_MODE = BotStorageMode.LOCAL;

        BOT_STORAGE_SHEET_SYNC_INTERVAL_ms = Long.parseLong(String.valueOf(bot_storage_sheet_sync_interval_sec)) * 1000;

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

    public static String getTelegramBotToken() {
        return TELEGRAM_BOT_TOKEN;
    }

    public static String getTelegramBotName() {
        return TELEGRAM_BOT_NAME;
    }

    public static long getBotStorageSheetSyncIntervalMilliSec() {
        return BOT_STORAGE_SHEET_SYNC_INTERVAL_ms;
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
}
