package telegram.bot.storage;

import java.util.List;

public interface StorageUtils {

    // format of cellAddress, rangeBegin, rangeEnd - "RXCY" (Row X Column Y).
    // RX may absent (CY in such a case means "get all cell until last non-empty cell in Column Y")
    // CY may absent (RX in such a case means "get all cell until last non-empty cell in Row X")

    boolean writeCellValue(String sheetName, String cellAddress, String cellValue);

    boolean writesValues(String sheetName, String cellAddress, List<List<Object>> values);

    List<String> reagValuesList(String sheetName, String rangeBegin, String rangeEnd);

    List<List<String>> readValuesRange(String sheetName, String rangeBegin, String rangeEnd);
}

