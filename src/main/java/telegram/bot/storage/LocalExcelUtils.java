package telegram.bot.storage;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import telegram.bot.config.SheetConfig;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class LocalExcelUtils implements StorageUtils {
    private final String pathToExcelFile;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void writeContactsToExcel(Map<String, User> contacts) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        //   Sheet sheet = workbook.createSheet("Contacts");
        Sheet sheet = workbook.createSheet(SheetConfig.getSheetContacts());
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);

        Row header = sheet.createRow(0);
        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Name");
        headerCell = header.createCell(1);
        headerCell.setCellValue("Login Telegram");
        headerCell = header.createCell(2);
        headerCell.setCellValue("Phone");

        AtomicInteger countRow = new AtomicInteger(1);
        contacts.forEach((key, value) -> {
            Row row = sheet.createRow(countRow.get());
            Cell cell = row.createCell(0);
            cell.setCellValue(key);
            cell = row.createCell(1);
            cell.setCellValue(value.getTelegram());
            cell = row.createCell(2);
            cell.setCellValue(value.getCode());
            countRow.getAndIncrement();
        });

        try (FileOutputStream outputStream = new FileOutputStream(pathToExcelFile)) {
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }
    }

    public void writeVolunteersToExcel(Map<LocalDate, Event> events) {
        // Add a sheet into Existing workbook
        XSSFWorkbook workbook = null;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }
        Sheet sheet = workbook.createSheet(SheetConfig.getSheetVolunteers());
        //Формируем первую строку
        Row headRow = sheet.createRow(0);
        sheet.setColumnWidth(0, 6000);
        Cell cellHead = headRow.createCell(0);
        cellHead.setCellValue("Позиция");

        int maxRowNumber = findMaxRowNumber(events);
        //Формируем остальные строки
        for (int i = 1; i <= maxRowNumber; i++) {
            sheet.createRow(i);
        }
        events.forEach((key, value) -> {
            Cell headCell = headRow.createCell(value.getColumnNumber());
            headCell.setCellValue(key.format(DATE_FORMATTER));

            List<Participation> members = value.getParticipants();
            members.forEach(member -> {
                Row row = sheet.getRow(member.getRowNumber());
                Cell roleCell = (row.getCell(0) == null) ? row.createCell(0) : row.getCell(0);
                Cell cell = row.createCell(value.getColumnNumber());
                String cellValue = member.getUser() != null ? member.getUser().getFullName() : "";
                if (!member.getRole().isBlank()) {
                    roleCell.setCellValue(member.getRole());
                }
                cell.setCellValue(cellValue);
                System.out.println(cellValue);
            });

        });
        try (FileOutputStream fileOut = new FileOutputStream(pathToExcelFile)) {
            workbook.write(fileOut);
            fileOut.close();
            System.out.println("File is written successfully");
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }
    }

    public void writeVolunteersToExcelSave(Map<LocalDate, Event> volunteers) {
        // Add a sheet into Existing workbook
        XSSFWorkbook workbook = null;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (IOException e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }

        Sheet sheet = workbook.createSheet(SheetConfig.getSheetVolunteers());
        AtomicInteger countColumn = new AtomicInteger(0);
        //Формируем первую строку
        Row row = sheet.createRow(0);
        volunteers.forEach((key, value) -> {
            sheet.setColumnWidth(countColumn.get(), 6000);
            Cell cell = row.createCell(countColumn.get());
            cell.setCellValue(key.format(DATE_FORMATTER));
            countColumn.getAndIncrement();
        });
        try (FileOutputStream fileOut = new FileOutputStream(pathToExcelFile)) {
            workbook.write(fileOut);
            fileOut.close();
            System.out.println("File is written successfully");
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, List<String>> readXLSXFile(int indexSheet) {
        Map<Integer, List<String>> dataFromListExcel = new HashMap<>();
        try (InputStream ExcelFileToRead = new FileInputStream(pathToExcelFile); XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead)) {
            XSSFSheet sheet = wb.getSheetAt(indexSheet);
            XSSFRow row;
            XSSFCell cell;
            Iterator<Row> rows = sheet.rowIterator();
            int countRow = 0;
            while (rows.hasNext()) {
                List<String> fromRow = new ArrayList<>();
                row = (XSSFRow) rows.next();
                Iterator<Cell> cells = row.cellIterator();
                while (cells.hasNext()) {
                    cell = (XSSFCell) cells.next();
                    fromRow.add(cell.getStringCellValue());
                    System.out.print(cell.getStringCellValue() + " ");
                }
                dataFromListExcel.put(countRow, fromRow);
                countRow++;
                System.out.println();
            }
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }
        return dataFromListExcel;
    }

    @Override
    public boolean writeCellValue(String sheetName, String cellAddress, String cellValue) {
        // return writesValues(sheetName, cellAddress, List.of(List.of(cellValue)));
        return false;
    }

    @Override
    public boolean writesValues(String sheetName, String cellAddress, List<List<Object>> values) {
        System.out.println();
        /*
 try {
            var range = sheetName + "!" + cellAddress;
            var body = new ValueRange().setValues(values);
            UpdateValuesResponse result = sheetService.spreadsheets().values()
                    .update(SheetConfig.getGoogleSheetId(), range, body)
                    .setValueInputOption("RAW")
                    .execute();
            pause();
        } catch (IOException e) {
            throw new RuntimeException(e);
            //return false;
        }
        return true;
         */
        return false;
    }

    @Override
    public List<String> readValuesList(String sheetName, String rangeBegin, String rangeEnd) {
        return readValuesFromList(sheetName, rangeBegin, rangeEnd, 0);
    }

    @Override
    public List<List<String>> readValuesRange(String sheetName, String rangeBegin, String rangeEnd) {
        List<List<String>> values = new LinkedList<>();
        Map<Integer, List<String>> dataFromListExcel = new HashMap<>();
        try (InputStream ExcelFileToRead = new FileInputStream(pathToExcelFile); XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead)) {
            XSSFSheet sheet = wb.getSheet(sheetName);
            int columnCount = columnCount(sheet);
            int rowCount = sheet.getLastRowNum();
            int rangeEndRow = addressCell(rangeEnd, 1, rowCount);
            int rangeEndColumn = addressCell(rangeEnd, 2, columnCount);
            XSSFRow row;
            XSSFCell cell;
            Iterator<Row> rows = sheet.rowIterator();
            int skipRowNumbers = addressCell(rangeBegin, 1, 0);
            int rowNum = 0;
            while (rows.hasNext() && rowNum <= rangeEndRow) {
                while (rowNum < skipRowNumbers - 1) {
                    if (rows.hasNext()) {
                        row = (XSSFRow) rows.next();
                        row.getLastCellNum();
                        rowNum++;
                    }
                }
                int skipColumnNumbers = addressCell(rangeBegin, 2, 0);
                int columnNum = 0;
                List<String> fromRow = new ArrayList<>();
                row = (XSSFRow) rows.next();
                Iterator<Cell> cells = row.cellIterator();
                while (cells.hasNext() && columnNum <= rangeEndColumn) {
                    while (columnNum < skipColumnNumbers - 1) {
                        if (cells.hasNext()) {
                            cell = (XSSFCell) cells.next();
                            columnNum++;
                        }
                    }
                    cell = (XSSFCell) cells.next();
                    fromRow.add(cell.getStringCellValue());
                    System.out.print(cell.getStringCellValue() + " ");
                    columnNum++;
                }
                dataFromListExcel.put(rowNum, fromRow);
                values.add(fromRow);
                rowNum++;
            }
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }
        return values;
    }

    private List<String> readValuesFromList(String sheetName, String rangeBegin, String rangeEnd, int index) {
        List<String> valuesList = new LinkedList<>();
        readValuesRange(sheetName, rangeBegin, rangeEnd).forEach(values -> valuesList.add(!values.isEmpty() && values.size() >= index + 1 ? values.get(index) : ""));
        return valuesList;
    }

    private int findMaxRowNumber(Map<LocalDate, Event> events) {
        Event maxEvent = events.values().stream().max(Comparator.comparingInt(event -> event.getParticipants().stream()
                .max(Comparator.comparingInt(Participation::getRowNumber)).get().getRowNumber())).get();

        return maxEvent.getParticipants().stream().max(Comparator.comparingInt(Participation::getRowNumber)).get().getRowNumber();
    }

    private int addressCell(String rangeBegin, int numberGroup, int defaultValue) {
        Pattern pattern = Pattern.compile(".*?(\\d+).*(\\d+).*");
        Matcher matcher = pattern.matcher(rangeBegin);
        String numberStr = String.valueOf(defaultValue);
        if (matcher.find()) {
            numberStr = matcher.group(numberGroup);
        }
        return Integer.parseInt(numberStr);
    }

    private int columnCount(XSSFSheet sheet) {
        XSSFRow row;
        int max = 0;
        Iterator<Row> rows = sheet.rowIterator();
        while (rows.hasNext()) {
            row = (XSSFRow) rows.next();
            int lastCellNum = row.getLastCellNum();
            if (lastCellNum > max) {
                max = lastCellNum;
            }
        }
        return max;
    }
}