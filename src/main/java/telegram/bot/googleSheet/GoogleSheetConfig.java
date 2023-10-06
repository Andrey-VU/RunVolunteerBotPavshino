package telegram.bot.googleSheet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:application.properties")
@PropertySource(value = "file:${LOCAL_CONFIG_DIR}/googleSheet.properties", ignoreResourceNotFound = false)
public class GoogleSheetConfig {
    private static String APPLICATION_NAME;
    private static String SERVICE_ACCOUNT_KEY_PATH;

    public GoogleSheetConfig(@Value("${google.sheet.app.name}") String google_sheet_app_name,
                             @Value("${google.sheet.service.account.key}") String google_sheet_service_account_key) {
        APPLICATION_NAME = google_sheet_app_name;
        SERVICE_ACCOUNT_KEY_PATH = google_sheet_service_account_key;
    }

    public static String getApplicationName() {
        return APPLICATION_NAME;
    }

    public static String getServiceAccountKeyPath() {
        return SERVICE_ACCOUNT_KEY_PATH;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
