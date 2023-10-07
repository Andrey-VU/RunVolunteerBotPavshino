package telegram.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/googleSheet.properties", ignoreResourceNotFound = false)
public class GoogleSheetConfig {
    private static String APPLICATION_NAME;
    private static String SERVICE_ACCOUNT_KEY_PATH;
    private static String SHEET_ID;
    private static String SHEET_NAME_VOLUNTEERS;
    private static String SHEET_NAME_CONTACTS;
    private static int SHEET_CONTACTS_START_ROW;
    private static int SHEET_ROLE_START_ROW;
    private static int SHEET_EVENT_START_COLUMN;
    private static int SHEET_SATURDAYS_AHEAD;
    private static long API_PAUSE_LONG;

    public GoogleSheetConfig(@Value("${sheet.app.name}") String sheet_app_name,
                             @Value("${sheet.service.account.key}") String sheet_service_account_key,
                             @Value("${sheet.id}") String sheet_id,
                             @Value("${sheet.name.volunteers}") String sheet_name_volunteers,
                             @Value("${sheet.name.contacts}") String sheet_name_contacts,
                             @Value("${sheet.contacts.start.row}") String sheet_contacts_start_row,
                             @Value("${sheet.role.start.row}") String sheet_role_start_row,
                             @Value("${sheet.event.start.column}") String sheet_event_start_column,
                             @Value("${sheet.saturdays.ahead}") String sheet_saturdays_ahead,
                             @Value("${api.pause.ms}") String api_pause_ms) {
        APPLICATION_NAME = sheet_app_name;
        SERVICE_ACCOUNT_KEY_PATH = sheet_service_account_key;
        SHEET_ID = sheet_id;
        SHEET_NAME_VOLUNTEERS = sheet_name_volunteers;
        SHEET_NAME_CONTACTS = sheet_name_contacts;
        SHEET_CONTACTS_START_ROW = Integer.parseInt(sheet_contacts_start_row);
        SHEET_ROLE_START_ROW = Integer.parseInt(sheet_role_start_row);
        SHEET_EVENT_START_COLUMN = Integer.parseInt(sheet_event_start_column);
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

    public static String getSheetNameVolunteers() {
        return SHEET_NAME_VOLUNTEERS;
    }

    public static String getSheetNameContacts() {
        return SHEET_NAME_CONTACTS;
    }

    public static int getSheetContactsStartRow() {
        return SHEET_CONTACTS_START_ROW;
    }

    public static int getSheetRoleStartRow() {
        return SHEET_ROLE_START_ROW;
    }

    public static int getSheetEventStartColumn() {
        return SHEET_EVENT_START_COLUMN;
    }

    public static int getSheetSaturdaysAhead() {
        return SHEET_SATURDAYS_AHEAD;
    }

    public static long getApiPauseLong() {
        return API_PAUSE_LONG;
    }


    @Override
    public String toString() {
        return super.toString();
    }
}
