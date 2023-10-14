package telegram.bot.storage;

import java.util.List;

public interface StorageUtils {
    boolean writeCellValue(String sheetName, String cellAddress, String cellValue);

    boolean writesValues(String sheetName, String cellAddress, List<List<Object>> values);

    List<String> reagValuesList(String sheetName, String rangeBegin, String rangeEnd);

    List<String> reagValuesList(String sheetName, String rangeBegin, String rangeEnd, int index);

    List<List<String>> readValuesRange(String sheetName, String rangeBegin, String rangeEnd);
}
