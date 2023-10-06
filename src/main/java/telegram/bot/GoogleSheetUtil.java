package telegram.bot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleSheetUtil {
    private final String APPLICATION_NAME;
    private final Credential credential;
    private final NetHttpTransport HTTP_TRANSPORT;
    private final Sheets sheetService;

    public GoogleSheetUtil() {
        this.APPLICATION_NAME = "Google Sheets App";

        var pathToKey = "E:\\YandexDisk (noir74)\\Sync\\YandexDisk\\IdeaProjects\\RunVolunteerBotPavshino\\src\\main\\resources\\pavshino5verstServiceAccount.json";
        GoogleCredentials googleCredentials;
        HttpRequestInitializer requestInitializer;

        try {
            googleCredentials = GoogleCredentials.
                    fromStream(new FileInputStream(pathToKey));
            requestInitializer = new HttpCredentialsAdapter(googleCredentials);

            this.credential = GoogleAuthorizeUtil.getGoogleCredential();
            this.HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        this.sheetService = new Sheets.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
