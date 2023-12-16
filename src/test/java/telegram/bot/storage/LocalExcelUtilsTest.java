package telegram.bot.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import telegram.bot.storage.google.GoogleSheetUtils;

import java.util.List;


@ExtendWith(SpringExtension.class)
@TestPropertySource("file:${RunVolunteerBotPavshinoLocalConfigDir}/sheet.properties")
class LocalExcelUtilsTest {
    private GoogleSheetUtils googleSheetUtils;
    private LocalExcelUtils localExcelUtils;

    @Value("${local.storage.path}")
    private String pathToExcelFile;

//    @Test
//    void initExcelFileTest() {
//        System.out.println(pathToExcelFile);
//        assertThat(pathToExcelFile, equalTo("$MODULE_WORKING_DIR$\\local_config\\localstorage.xlsx"));
//        System.out.println();
//    }


    @Test
    void writeCellValue() {
        //  GoogleSheetUtils googleSheetUtils = new GoogleSheetUtils()
        localExcelUtils = new LocalExcelUtils(pathToExcelFile);
        localExcelUtils.writeCellValue("Волонтеры", "R3C3", "TestValue");
    }

    @Test
    void writesValues() {
        localExcelUtils = new LocalExcelUtils(pathToExcelFile);
        List<List<Object>> user = List.of(List.of("Тест Имя", "@testTelegram", "1234567890"));
        localExcelUtils.writesValues("Контакты", "R3C1", user);
    }

    @Test
    void readValuesList() {
    }

    @Test
    void readValuesRange() {
    }

    @Test
    void readXLSXFile() {
    }
}