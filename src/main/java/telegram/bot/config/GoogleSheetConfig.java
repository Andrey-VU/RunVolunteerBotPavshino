package telegram.bot.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import telegram.bot.storage.GoogleSheetUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;

@Configuration
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/googleSheet.properties", ignoreResourceNotFound = false)
public class GoogleSheetConfig {
    private static String APPLICATION_NAME;
    private static String SERVICE_ACCOUNT_KEY_PATH;
    private static String SHEET_ID;
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
    private static long API_PAUSE_LONG;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public GoogleSheetConfig(@Value("${sheet.app.name}") String sheet_app_name,
                             @Value("${sheet.service.account.key}") String sheet_service_account_key,
                             @Value("${sheet.id}") String sheet_id,
                             @Value("${sheet.volunteers}") String sheet_volunteers,
                             @Value("${sheet.contacts}") String sheet_contacts,
                             @Value("${sheet.contacts.row.start}") String sheet_contacts_row_start,
                             @Value("${sheet.contacts.column.first}") String sheet_contacts_column_first,
                             @Value("${sheet.contacts.column.last}") String sheet_contacts_column_last,
                             @Value("${sheet.volunteers.role.row.start}") String sheet_volunteers_role_row_start,
                             @Value("${sheet.volunteers.role.column}") String sheet_volunteers_role_column,
                             @Value("${sheet.volunteers.event.column.start}") String sheet_volunteers_event_column_start,
                             @Value("${sheet.volunteers.event.row}") String sheet_volunteers_event_row,
                             @Value("${sheet.saturdays.ahead}") String sheet_saturdays_ahead,
                             @Value("${api.pause.ms}") String api_pause_ms) {
        APPLICATION_NAME = sheet_app_name;
        SERVICE_ACCOUNT_KEY_PATH = sheet_service_account_key;
        SHEET_ID = sheet_id;
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
        API_PAUSE_LONG = Long.parseLong(api_pause_ms);
    }

    public static String getApplicationName() {
        return APPLICATION_NAME;
    }

    public static String getServiceAccountKeyPath() {
        return SERVICE_ACCOUNT_KEY_PATH;
    }

    public static String getSheetId() {
        return SHEET_ID;
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

    public static long getApiPauseLong() {
        return API_PAUSE_LONG;
    }

    @Bean(name = "GoogleSheetUtils")
    public GoogleSheetUtils getGoogleSheetUtils() {
        return new GoogleSheetUtils(connectToStorage());
    }

    private Sheets connectToStorage() {
        GoogleCredentials googleCredentials;
        HttpRequestInitializer requestInitializer;
        NetHttpTransport netHttpTransport;

        try {
            System.out.println(GoogleSheetConfig.getServiceAccountKeyPath());
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

    @Override
    public String toString() {
        return super.toString();
    }
}
