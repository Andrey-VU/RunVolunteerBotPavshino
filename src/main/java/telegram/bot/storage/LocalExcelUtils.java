package telegram.bot.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.model.Volunteer;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class LocalExcelUtils implements StorageUtils {
    private final String pathToExcelFile;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void initExcelFile(Map<String, Volunteer> contacts, Map<LocalDate, Event> events) {
        /*File f = new File(pathToExcelFile);
        if (!f.exists()) {
            writeContactsToExcel(contacts);
            writeVolunteersToExcel(events);
        } else {
            log.info("File exists");
        }*/
        File f = new File(pathToExcelFile);
        //if (!f.exists()) {
        writeContactsToExcel(contacts);
        writeVolunteersToExcel(events);
        // } else {
        //  log.info("File exists");
        //}
    }

    @Override
    public boolean writeCellValue(String sheetName, String cellAddress, String cellValue) {
        return writesValues(sheetName, cellAddress, List.of(List.of(cellValue)));
    }

    @Override
    public boolean writesValues(String sheetName, String cellAddress, List<List<Object>> values) {
        File file = new File(pathToExcelFile);
        if (!file.exists()) {
            log.info("File not found");
            return false;
        }
        XSSFWorkbook workbook;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (IOException e) {
            log.info("Exception while updating an existing excel file.");
            throw new RuntimeException(e);
        }
        // Проверка существования листа
        int sheetIndex = workbook.getSheetIndex(sheetName);
        if (sheetIndex != -1) {
            Sheet sheet = workbook.getSheet(sheetName);
            int offsetRow = getNumberCell(cellAddress, 1, -1) - 1;
            int offsetCell = getNumberCell(cellAddress, 2, -1) - 1;
            for (int i = 0; i < values.size(); i++) {
                for (int j = 0; j < values.get(i).size(); j++) {
                    Row row = sheet.getRow(i + offsetRow);
                    if (row == null) {
                        row = sheet.createRow(i + offsetRow);
                    }
                    Cell cell = row.getCell(j + offsetCell);
                    if (cell == null) {
                        cell = row.createCell(j + offsetCell);
                    }
                    cell.setCellValue(values.get(i).get(j).toString());
                }
            }
        } else {
            log.info("Лист '" + sheetName + "' не существует.");
            return false;
        }
        try (FileOutputStream outputStream = new FileOutputStream(pathToExcelFile)) {
            workbook.write(outputStream);
            workbook.close();
            return true;
        } catch (Exception e) {
            log.info("Error creating FileOutputStream");
            throw new RuntimeException(e);
        }
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

            int rangeEndRow = getNumberCell(rangeEnd, 1, rowCount);
            int rangeEndColumn = getNumberCell(rangeEnd, 2, columnCount);
            XSSFRow row;
            XSSFCell cell;
            Iterator<Row> rows = sheet.rowIterator();
            int skipRowNumbers = getNumberCell(rangeBegin, 1, 0);
            int rowNum = 0;
            while (rows.hasNext() && rowNum <= rangeEndRow) {
                while (rowNum < skipRowNumbers - 1) {
                    if (rows.hasNext()) {
                        row = (XSSFRow) rows.next();
                        row.getLastCellNum();
                        rowNum++;
                    }
                }
                int skipColumnNumbers = getNumberCell(rangeBegin, 2, 0);
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
                    columnNum++;
                }
                dataFromListExcel.put(rowNum, fromRow);
                values.add(fromRow);
                rowNum++;
            }
        } catch (Exception e) {
            log.info("Error creating FileInputStream");
            throw new RuntimeException(e);
        }
        return values;
    }

    private void writeContactsToExcel(Map<String, Volunteer> contacts) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet(BotConfiguration.getSheetContacts());
        for (int i = 0; i < 4; i++) {
            sheet.setColumnWidth(i, 6000);
        }
        Row header = sheet.createRow(0);
        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Name");
        headerCell = header.createCell(1);
        headerCell.setCellValue("Login Telegram");
        headerCell = header.createCell(2);
        headerCell.setCellValue("Phone");
        headerCell = header.createCell(3);
        headerCell.setCellValue("Comment");
        AtomicInteger countRow = new AtomicInteger(1);
        contacts.forEach((key, value) -> {
            Row row = sheet.createRow(countRow.get());
            Cell cell = row.createCell(0);
            cell.setCellValue(key);
            cell = row.createCell(1);
            cell.setCellValue(value.getTgUserName());
            cell = row.createCell(2);
            cell.setCellValue(value.getCode());
            cell = row.createCell(3);
            cell.setCellValue(value.getComment());
            countRow.getAndIncrement();
        });
        try (FileOutputStream outputStream = new FileOutputStream(pathToExcelFile)) {
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            log.info("Error creating FileOutputStream");
            throw new RuntimeException(e);
        }
    }

    private void writeVolunteersToExcel(Map<LocalDate, Event> events) {
        int rowNumberCorrection = findMinRowNumber(events) - 1;
        int colNumberCorrection = findMinColNumber(events) - 1;
        // Add a sheet into Existing workbook
        XSSFWorkbook workbook = null;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (Exception e) {
            log.info("Error creating FileInputStream");
            throw new RuntimeException(e);
        }
        Sheet sheet = workbook.createSheet(BotConfiguration.getSheetVolunteers());
        //Формируем первую строку
        Row headRow = sheet.createRow(0);
        sheet.setColumnWidth(0, 6000);
        Cell cellHead = headRow.createCell(0);
        cellHead.setCellValue("Позиция");

        int maxRowNumber = findMaxRowNumber(events);
        //Формируем остальные строки
        for (int i = 1; i <= maxRowNumber - rowNumberCorrection; i++) {
            sheet.createRow(i);
        }
        events.forEach((key, value) -> {
            Cell headCell = headRow.createCell(value.getColumnNumber() - colNumberCorrection);
            headCell.setCellValue(key.format(DATE_FORMATTER));

            List<Participation> members = value.getParticipants();
            members.forEach(member -> {
                Row row = sheet.getRow(member.getSheetRowNumber() - rowNumberCorrection);
                Cell roleCell = (row.getCell(0) == null) ? row.createCell(0) : row.getCell(0);
                Cell cell = row.createCell(value.getColumnNumber() - colNumberCorrection);
                String cellValue = member.getVolunteer() != null ? member.getVolunteer().getFullName() : "";
                if (!member.getEventRole().isBlank()) {
                    roleCell.setCellValue(member.getEventRole());
                }
                cell.setCellValue(cellValue);
            });

        });
        try (FileOutputStream fileOut = new FileOutputStream(pathToExcelFile)) {
            workbook.write(fileOut);
            fileOut.close();
            log.info("File is written successfully");
        } catch (Exception e) {
            log.info("Error creating FileOutputStream");
            throw new RuntimeException(e);
        }
    }

    public void writeVolunteersToExcelSave(Map<LocalDate, Event> volunteers) {
        // Add a sheet into Existing workbook
        XSSFWorkbook workbook = null;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (IOException e) {
            log.info("Error creating FileInputStream");
            throw new RuntimeException(e);
        }
        Sheet sheet = workbook.createSheet(BotConfiguration.getSheetVolunteers());
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
            log.info("File is written successfully");
        } catch (Exception e) {
            log.info("Error creating FileOutputStream");
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
                }
                dataFromListExcel.put(countRow, fromRow);
                countRow++;
            }
        } catch (Exception e) {
            log.info("Error creating FileInputStream");
            throw new RuntimeException(e);
        }
        return dataFromListExcel;
    }


    private List<String> readValuesFromList(String sheetName, String rangeBegin, String rangeEnd, int index) {
        List<String> valuesList = new LinkedList<>();
        readValuesRange(sheetName, rangeBegin, rangeEnd).forEach(values -> valuesList.add(!values.isEmpty() && values.size() >= index + 1 ? values.get(index) : ""));
        return valuesList;
    }

    private int findMaxRowNumber(Map<LocalDate, Event> events) {
        Event maxEvent = events.values().stream().max(Comparator.comparingInt(event -> event.getParticipants().stream()
                .max(Comparator.comparingInt(Participation::getSheetRowNumber)).get().getSheetRowNumber())).get();

        return maxEvent.getParticipants().stream().max(Comparator.comparingInt(Participation::getSheetRowNumber)).get().getSheetRowNumber();
    }

    private int findMinRowNumber(Map<LocalDate, Event> events) {
        Event minEvent = events.values().stream().min(Comparator.comparingInt(event -> event.getParticipants().stream()
                .min(Comparator.comparingInt(Participation::getSheetRowNumber)).get().getSheetRowNumber())).get();

        return minEvent.getParticipants().stream().min(Comparator.comparingInt(Participation::getSheetRowNumber)).get().getSheetRowNumber();
    }

    private int findMinColNumber(Map<LocalDate, Event> events) {
        Event minColumnNumberEvent = events.values().stream().min(Comparator.comparingInt(event -> event.getColumnNumber())).get();
        return minColumnNumberEvent.getColumnNumber();
    }

    private int addressPartCell(String range, int numberPart, int defaultValue) {
        if (range == null) {
            return defaultValue;
        }
        CellReference cr = new CellReference(range);
        int[] address = new int[]{cr.getRow(), cr.getCol()};

        return address[numberPart];
    }

    private int getNumberCell(String rangeBegin, int numberGroup, int defaultValue) {
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