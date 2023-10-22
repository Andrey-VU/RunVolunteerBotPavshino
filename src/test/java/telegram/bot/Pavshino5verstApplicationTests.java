package telegram.bot;

import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.LocalDate;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Pavshino5verstApplicationTests {
    private static TelegramBotStorage telegramBotStorage;

    @BeforeAll
    static void init() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(BotConfiguration.class);
        telegramBotStorage = ctx.getBean(TelegramBotStorage.class);
    }

    @Test
    @Order(1)
    void saveUserTest() {
        telegramBotStorage.saveUser(
                User.builder()
                        .name("Петя")
                        .surname("ИВАНОВ")
                        .telegram("@ivanov")
                        .code("0000").build());
    }


    @Test
    @Order(2)
    void saveParticipationTest() {
        var participation = Participation.builder()
                .user(User.builder()
                        .name("Петя")
                        .surname("ИВАНОВ").build()
                )
                .eventDate(string2LocalDate("21.10.2023"))
                .eventRole("Маршал2 дальний разворот")
                .sheetRowNumber(7)
                .build();
        telegramBotStorage.saveParticipation(participation);
    }

    private LocalDate string2LocalDate(String value) {
        return LocalDate.parse(value, BotConfiguration.DATE_FORMATTER);
    }
}
