package telegram.bot.adapter.google;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AllArgsConstructor
class TelegramBotStorageGoogleTableImplTest {
//private TelegramBotStorageGoogleTableImpl telegramBotStorageGoogleTable;
    @Test
    void loadDataFromStorage() {
       User user = User.builder()
               .telegram("@telegram")
                        .name("Елена")
                        .surname("ДЕНИСКИН").build();

       // telegramBotStorageGoogleTable.getUserByTelegram("@triplex35");
        System.out.println();
    }
}