package telegram.bot.googleSheet;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Data
@Component
public class GoogleSheetUtils {
    private Sheets sheetService;

    public GoogleSheetUtils() {
        initialize();
    }

    public void writeCellValue(String sheetName, String cellAddress, String cellValue) {
        try {
            var range = sheetName + "!" + cellAddress;
            var value = new ValueRange().setValues(List.of(List.of(cellValue)));
            UpdateValuesResponse result = sheetService.spreadsheets().values()
                    .update(GoogleSheetConfig.getGoogleSheetId(), range, value)
                    .setValueInputOption("RAW")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readCellValue(String sheetName, String cellAddress) {
        return readRangeValues(sheetName, cellAddress, cellAddress).get(0).get(0);
    }

    public List<List<String>> readRangeValues(String sheetName, String rangeBegin, String rangeEnd) {
        List<List<String>> values = new LinkedList<>();
        try {
            var range = sheetName + "!" + rangeBegin + ":" + rangeEnd;

            var objectsInTheRange = Optional
                    .ofNullable(
                            sheetService.spreadsheets()
                                    .values()
                                    .get(GoogleSheetConfig.getGoogleSheetId(), range)
                                    .execute()
                                    .getValues())
                    .orElse(List.of(List.of("")));

            objectsInTheRange
                    .forEach(currentRowObject -> {
                        var rowStringList = new LinkedList<String>();
                        currentRowObject.forEach(currentCellObject -> rowStringList.add(Optional.ofNullable(currentCellObject).orElse("").toString()));
                        values.add(rowStringList);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return values;
    }

    private void initialize() {
        connect();
        prepareData();
    }

    private void connect() {
        GoogleCredentials googleCredentials;
        HttpRequestInitializer requestInitializer;
        NetHttpTransport netHttpTransport;

        try {
            googleCredentials = GoogleCredentials.fromStream(new FileInputStream(GoogleSheetConfig.getServiceAccountKeyPath()));
            requestInitializer = new HttpCredentialsAdapter(googleCredentials);
            netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        this.sheetService = new Sheets.Builder(netHttpTransport, GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName(GoogleSheetConfig.getApplicationName())
                .build();
    }

    private void prepareData() {
        writeCellValue(GoogleSheetConfig.getGoogleSheetNameVolunteers(), "C17", "test тест");
        System.out.println(readCellValue(GoogleSheetConfig.getGoogleSheetNameVolunteers(), "C17"));
    }
}
