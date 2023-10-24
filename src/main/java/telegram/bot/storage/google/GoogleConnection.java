package telegram.bot.storage.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import telegram.bot.config.BotConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleConnection {
    NetHttpTransport netHttpTransport;
    GoogleCredentials googleCredentials;

    public GoogleConnection() {
        try {
            googleCredentials = GoogleCredentials.fromStream(new FileInputStream(BotConfiguration.getGoogleServiceAccountKeyPath()));
            netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public Sheets getSheetService() {
        return
                new Sheets.Builder(
                        netHttpTransport,
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(googleCredentials))
                        .setApplicationName(BotConfiguration.getGoogleApplicationName())
                        .build();
    }

    public Drive getDriveService() {
        return
                new Drive.Builder(
                        netHttpTransport,
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(googleCredentials.createScoped(Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY))))
                        .setApplicationName("BotConfiguration.getGoogleApplicationName()")
                        .build();
    }
}
