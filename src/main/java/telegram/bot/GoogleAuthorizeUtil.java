package telegram.bot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleAuthorizeUtil {
    public static Credential authorize() throws IOException, GeneralSecurityException {
        InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream("/pavshino5verstOAuthClient.json");
        assert in != null;
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
                .Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), clientSecrets, List.of(SheetsScopes.SPREADSHEETS))
                .setDataStoreFactory(new MemoryDataStoreFactory())
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static GoogleCredential getGoogleCredential() throws IOException {
        var pathToKey = "E:\\YandexDisk (noir74)\\Sync\\YandexDisk\\IdeaProjects\\RunVolunteerBotPavshino\\src\\main\\resources\\pavshino5verstServiceAccount.json";
        GoogleCredentials googleCredentials = GoogleCredentials.
                fromStream(new FileInputStream(pathToKey));
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(googleCredentials);

        return GoogleCredential
                .fromStream(new FileInputStream(pathToKey))
                .createScoped(List.of(SheetsScopes.SPREADSHEETS));
    }
}