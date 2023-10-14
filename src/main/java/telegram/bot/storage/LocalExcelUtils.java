package telegram.bot.storage;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
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

@RequiredArgsConstructor
public class LocalExcelUtils {
    private final String pathToExcelFile;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void writeContactsToExcel(Map<String, User> contacts) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Contacts");
        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);

        Row header = sheet.createRow(0);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setBold(false);
        headerStyle.setFont(font);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Name");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(1);
        headerCell.setCellValue("Login Telegram");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(2);
        headerCell.setCellValue("Phone");
        headerCell.setCellStyle(headerStyle);

        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);

        AtomicInteger countRow = new AtomicInteger(1);

        contacts.forEach((key, value) -> {
            Row row = sheet.createRow(countRow.get());
            Cell cell = row.createCell(0);
            cell.setCellValue(key);
            cell.setCellStyle(style);

            cell = row.createCell(1);
            cell.setCellValue(value.getTelegram());
            cell.setCellStyle(style);

            cell = row.createCell(2);
            cell.setCellValue(value.getCode());
            cell.setCellStyle(style);

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

    public void writeVolunteersToExcelSave(Map<LocalDate, Event> volunteers) {
        // Add a sheet into Existing workbook
        XSSFWorkbook workbook = null;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (IOException e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }

        Sheet sheet = workbook.createSheet("Volunteers");
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

    public void writeVolunteersToExcel(Map<LocalDate, Event> events) {
        // Add a sheet into Existing workbook
        XSSFWorkbook workbook = null;
        try (FileInputStream fileinp = new FileInputStream(pathToExcelFile)) {
            workbook = new XSSFWorkbook(fileinp);
        } catch (Exception e) {
            // TODO - add logging
            throw new RuntimeException(e);
        }

        Sheet sheet = workbook.createSheet("Volunteers");

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
            members.forEach(
                    member -> {
                        Row row = sheet.getRow(member.getRowNumber());
                        Cell cell = row.createCell(value.getColumnNumber());
                        String cellValue = member.getUser() != null ? member.getUser().getFullName() : "";

                        cell.setCellValue(cellValue);
                        System.out.println(cellValue);
                    }
            );

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
        try (InputStream ExcelFileToRead = new FileInputStream(pathToExcelFile);
             XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead)) {

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

    private int findMaxRowNumber(Map<LocalDate, Event> events) {
        Event maxEvent = events.values().stream()
                .max(Comparator.comparingInt(event -> event.getParticipants().stream().max(Comparator.comparingInt(Participation::getRowNumber)).get().getRowNumber()))
                .get();

        return maxEvent.getParticipants().stream().max(Comparator.comparingInt(Participation::getRowNumber)).get().getRowNumber();
    }
}
