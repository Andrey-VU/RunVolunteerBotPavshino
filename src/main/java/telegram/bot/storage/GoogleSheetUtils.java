package telegram.bot.storage;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import telegram.bot.config.GoogleSheetConfig;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GoogleSheetUtils {
    private final Sheets sheetService;

    public GoogleSheetUtils(Sheets sheetService) {
        this.sheetService = sheetService;
    }

    public void writeCellValue(String sheetName, String cellAddress, String cellValue) {
        try {
            var range = sheetName + "!" + cellAddress;
            var value = new ValueRange().setValues(List.of(List.of(cellValue)));
            UpdateValuesResponse result = sheetService.spreadsheets().values()
                    .update(GoogleSheetConfig.getSheetId(), range, value)
                    .setValueInputOption("RAW")
                    .execute();
            pause();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readValueCell(String sheetName, String cellAddress) {
        return readValuesArray(sheetName, cellAddress, cellAddress).get(0).get(0);
    }

    public List<String> readValuesRange(String sheetName, String rangeBegin, String rangeEnd) {
        return readValuesArray(sheetName, rangeBegin, rangeEnd).get(0);
    }

    public List<List<String>> readValuesArray(String sheetName, String arrayBegin, String arrayEnd) {
        List<List<String>> values = new LinkedList<>();
        try {
            var arrayBorders = sheetName + "!" + arrayBegin + ":" + arrayEnd;

            var objectsInTheRange = Optional
                    .ofNullable(
                            sheetService.spreadsheets()
                                    .values()
                                    .get(GoogleSheetConfig.getSheetId(), arrayBorders)
                                    .execute()
                                    .getValues())
                    .orElse(List.of(List.of("")));

            pause();

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

    private void pause() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(GoogleSheetConfig.getApiPauseLong());
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
