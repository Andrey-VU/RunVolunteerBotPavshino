package telegram.bot.storage;

import java.time.LocalDateTime;
import java.util.List;

public interface StorageUtils {

    // format of cellAddress, rangeBegin, rangeEnd - "RXCY" (Row X Column Y).
    // RX may absent (CY in such a case means "get all cell until last non-empty cell in Column Y")
    // CY may absent (RX in such a case means "get all cell until last non-empty cell in Row X")

    // writes 'cellValue' into 'cellAddress' at 'sheetName'
    boolean writeCellValue(String sheetName, String cellAddress, String cellValue);

    // writes martix 'values' starting from 'cellAddress' at 'sheetName'
    boolean writesValues(String sheetName, String cellAddress, List<List<Object>> values);

    // reads list of String from 'rangeBegin' to 'rangeEnd' at 'sheetName'
    List<String> readValuesList(String sheetName, String rangeBegin, String rangeEnd);

    // reads matrix of String from 'rangeBegin' to 'rangeEnd' at 'sheetName'
    List<List<String>> readValuesRange(String sheetName, String rangeBegin, String rangeEnd);

    default LocalDateTime getSheetLastUpdateTime() {
        return LocalDateTime.now().minusYears(1);
    }
}

