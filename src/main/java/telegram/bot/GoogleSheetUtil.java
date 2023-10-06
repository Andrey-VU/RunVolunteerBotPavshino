package telegram.bot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleSheetUtil {
    private final String APPLICATION_NAME;
    private final Credential credential;
    private final NetHttpTransport HTTP_TRANSPORT;
    private final Sheets sheetService;

    public GoogleSheetUtil() {
        this.APPLICATION_NAME = "Google Sheets App";
        try {
            //this.credential = GoogleAuthorizeUtil.getCredential();
            this.credential = GoogleAuthorizeUtil.getGoogleCredential();
            this.HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        this.sheetService = new Sheets.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
