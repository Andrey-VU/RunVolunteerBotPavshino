package telegram.bot;

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

public class GoogleSheetUtils {
    private Sheets sheetService;

    public GoogleSheetUtils() {
        initialize();
    }

    private void initialize() {
        var APPLICATION_NAME = "Google Sheets App";
        GoogleCredentials googleCredentials;
        HttpRequestInitializer requestInitializer;

        var pathToKey = "pavshino5verstServiceAccount.json";

        NetHttpTransport HTTP_TRANSPORT;
        try {
            googleCredentials = GoogleCredentials.
                    fromStream(new FileInputStream(pathToKey));
            requestInitializer = new HttpCredentialsAdapter(googleCredentials);

            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        this.sheetService = new Sheets.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
