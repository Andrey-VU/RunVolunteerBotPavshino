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
    private static String GOOGLE_SHEET_ID;
    private static String GOOGLE_SHEET_NAME_VOLUNTEERS;
    private static String GOOGLE_SHEET_NAME_CONTACTS;

    public GoogleSheetConfig(@Value("${google.sheet.app.name}") String google_sheet_app_name,
                             @Value("${google.sheet.service.account.key}") String google_sheet_service_account_key,
                             @Value("${google.sheet.id}") String google_sheet_id,
                             @Value("${google.sheet.name.volunteers}") String google_sheet_name_volunteers,
                             @Value("${google.sheet.name.contacts}") String google_sheet_name_contacts) {
        APPLICATION_NAME = google_sheet_app_name;
        SERVICE_ACCOUNT_KEY_PATH = google_sheet_service_account_key;
        GOOGLE_SHEET_ID = google_sheet_id;
        GOOGLE_SHEET_NAME_VOLUNTEERS = google_sheet_name_volunteers;
        GOOGLE_SHEET_NAME_CONTACTS = google_sheet_name_contacts;
    }

    public static String getApplicationName() {
        return APPLICATION_NAME;
    }

    public static String getServiceAccountKeyPath() {
        return SERVICE_ACCOUNT_KEY_PATH;
    }

    public static String getGoogleSheetId() {
        return GOOGLE_SHEET_ID;
    }

    public static String getGoogleSheetNameVolunteers() { return GOOGLE_SHEET_NAME_VOLUNTEERS; }

    public static String getGoogleSheetNameContacts() { return GOOGLE_SHEET_NAME_CONTACTS; }

    @Override
    public String toString() {
        return super.toString();
    }
}
