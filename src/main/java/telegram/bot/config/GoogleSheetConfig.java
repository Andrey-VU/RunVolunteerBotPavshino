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
    private static String SHEET_VOLUNTEERS;
    private static String SHEET_CONTACTS;
    private static int SHEET_CONTACTS_START_ROW;
    private static int SHEET_CONTACTS_START_COLUMN_BEGIN;
    private static int SHEET_CONTACTS_START_COLUMN_END;
    private static int SHEET_VOLUNTEERS_ROLE_START_ROW;
    private static int SHEET_VOLUNTEERS_ROLE_COLUMN;
    private static int SHEET_VOLUNTEERS_EVENT_START_COLUMN;
    private static int SHEET_VOLUNTEERS_EVENT_DATE_ROW;
    private static int SHEET_SATURDAYS_AHEAD;
    private static long API_PAUSE_LONG;

    public GoogleSheetConfig(@Value("${sheet.app.name}") String sheet_app_name,
                             @Value("${sheet.service.account.key}") String sheet_service_account_key,
                             @Value("${sheet.id}") String sheet_id,
                             @Value("${sheet.volunteers}") String sheet_volunteers,
                             @Value("${sheet.contacts}") String sheet_contacts,
                             @Value("${sheet.contacts.start.row}") String sheet_contacts_start_row,
                             @Value("${sheet.contacts.start.column.begin}") String sheet_contacts_start_column_begin,
                             @Value("${sheet.contacts.start.column.end}") String sheet_contacts_start_column_end,
                             @Value("${sheet.volunteers.role.start.row}") String sheet_volunteers_role_start_row,
                             @Value("${sheet.volunteers.role.column}") String sheet_volunteers_role_column,
                             @Value("${sheet.volunteers.event.start.column}") String sheet_volunteers_event_start_column,
                             @Value("${sheet.volunteers.event.date.row}") String sheet_volunteers_event_date_row,
                             @Value("${sheet.saturdays.ahead}") String sheet_saturdays_ahead,
                             @Value("${api.pause.ms}") String api_pause_ms) {
        APPLICATION_NAME = sheet_app_name;
        SERVICE_ACCOUNT_KEY_PATH = sheet_service_account_key;
        SHEET_ID = sheet_id;
        SHEET_VOLUNTEERS = sheet_volunteers;
        SHEET_CONTACTS = sheet_contacts;
        SHEET_CONTACTS_START_ROW = Integer.parseInt(sheet_contacts_start_row);
        SHEET_CONTACTS_START_COLUMN_BEGIN = Integer.parseInt(sheet_contacts_start_column_begin);
        SHEET_CONTACTS_START_COLUMN_END = Integer.parseInt(sheet_contacts_start_column_end);
        SHEET_VOLUNTEERS_ROLE_START_ROW = Integer.parseInt(sheet_volunteers_role_start_row);
        SHEET_VOLUNTEERS_ROLE_COLUMN = Integer.parseInt(sheet_volunteers_role_column);
        SHEET_VOLUNTEERS_EVENT_START_COLUMN = Integer.parseInt(sheet_volunteers_event_start_column);
        SHEET_VOLUNTEERS_EVENT_DATE_ROW = Integer.parseInt(sheet_volunteers_event_date_row);
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

    public static int getSheetContactsStartRow() {
        return SHEET_CONTACTS_START_ROW;
    }

    public static int getSheetContactsStartColumnBegin() {
        return SHEET_CONTACTS_START_COLUMN_BEGIN;
    }

    public static int getSheetContactsStartColumnEnd() {
        return SHEET_CONTACTS_START_COLUMN_END;
    }

    public static int getSheetVolunteersRoleStartRow() {
        return SHEET_VOLUNTEERS_ROLE_START_ROW;
    }

    public static int getSheetVolunteersRoleColumn() {
        return SHEET_VOLUNTEERS_ROLE_COLUMN;
    }

    public static int getSheetVolunteersEventStartColumn() {
        return SHEET_VOLUNTEERS_EVENT_START_COLUMN;
    }

    public static int getSheetVolunteersEventDateRow() {
        return SHEET_VOLUNTEERS_EVENT_DATE_ROW;
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
