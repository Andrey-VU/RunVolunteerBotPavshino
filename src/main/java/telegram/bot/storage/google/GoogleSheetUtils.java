package telegram.bot.storage.google;

import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import telegram.bot.config.BotConfiguration;
import telegram.bot.storage.StorageUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GoogleSheetUtils implements StorageUtils {
    private final Sheets sheetService;
    private final Drive driveService;

    public GoogleSheetUtils(GoogleConnection googleConnection) {
        sheetService = googleConnection.getSheetService();
        driveService = googleConnection.getDriveService();
    }

    @Override
    public boolean writeCellValue(String sheetName, String cellAddress, String cellValue) {
        return writesValues(sheetName, cellAddress, List.of(List.of(cellValue)));
    }

    @Override
    public boolean writesValues(String sheetName, String cellAddress, List<List<Object>> values) {
        try {
            var range = sheetName + "!" + cellAddress;
            var body = new ValueRange().setValues(values);
            UpdateValuesResponse result = sheetService.spreadsheets().values()
                    .update(BotConfiguration.getGoogleSheetId(), range, body)
                    .setValueInputOption("RAW")
                    .execute();
            pause();
        } catch (IOException e) {
            throw new RuntimeException(e);
            //return false;
        }
        return true;
    }

    @Override
    public List<String> readValuesList(String sheetName, String rangeBegin, String rangeEnd) {
        return readValuesList(sheetName, rangeBegin, rangeEnd, 0);
    }

    @Override
    public List<List<String>> readValuesRange(String sheetName, String rangeBegin, String rangeEnd) {
        List<List<String>> values = new LinkedList<>();
        try {
            var range = sheetName + "!" + rangeBegin + ":" + rangeEnd;

            var objectsInTheRange = Optional
                    .ofNullable(
                            sheetService.spreadsheets()
                                    .values()
                                    .get(BotConfiguration.getGoogleSheetId(), range)
                                    .execute()
                                    .getValues())
                    .orElse(Collections.emptyList());
            objectsInTheRange
                    .forEach(currentRowObject -> {
                        var rowStringList = new LinkedList<String>();
                        currentRowObject.forEach(currentCellObject -> rowStringList.add(Optional.ofNullable(currentCellObject).orElse("").toString()));
                        values.add(rowStringList);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pause();
        return values;
    }

    @Override
    public LocalDateTime getSheetLastUpdateTime() {
        LocalDateTime modifiedLocalDateTime;
        try {
            var modifiedDateTimeEpoch = driveService
                    .files()
                    .get(BotConfiguration.getGoogleSheetId())
                    .setFields("modifiedTime")
                    .execute()
                    .getModifiedTime()
                    .getValue();
            var modifiedDateTimeInstant = Instant.ofEpochMilli(modifiedDateTimeEpoch);
            modifiedLocalDateTime = modifiedDateTimeInstant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return modifiedLocalDateTime;
    }

    private List<String> readValuesList(String sheetName, String rangeBegin, String rangeEnd, int index) {
        List<String> valuesList = new LinkedList<>();
        readValuesRange(sheetName, rangeBegin, rangeEnd).forEach(values -> valuesList.add(!values.isEmpty() && values.size() >= index + 1 ? values.get(index) : ""));
        return valuesList;
    }

    private void pause() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(BotConfiguration.getGoogleApiPauseLong());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
