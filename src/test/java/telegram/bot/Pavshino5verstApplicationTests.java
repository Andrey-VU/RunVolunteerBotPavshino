package telegram.bot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.User;

import java.time.LocalDate;

//@SpringBootTest(classes = BotConfiguration.class)
class Pavshino5verstApplicationTests {

    private static TelegramBotStorage telegramBotStorage;

    @BeforeAll
    static void init() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(BotConfiguration.class);
        telegramBotStorage = ctx.getBean(TelegramBotStorage.class);
    }

    @Test
    void saveUserTest() {
        telegramBotStorage.saveUser(
                User.builder()
                        .name("Петя")
                        .surname("Иванов")
                        .telegram("@noir74")
                        .code("74").build());
    }


    @Test
    void saveParticipationTest() {
//        var participation = Participation.builder()
//                .user(User.builder()
//                        .name("Елена")
//                        .surname("ДЕНИСКИН").build()
//                )
//                .eventDate(string2LocalDate("22.04.2023"))
//                .role("Фотограф2")
//                .rowNumber(7)
//                .build();
        //telegramBotStorage.saveParticipation(participation);
    }

    private LocalDate string2LocalDate(String value) {
        return LocalDate.parse(value, BotConfiguration.DATE_FORMATTER);
    }
}
