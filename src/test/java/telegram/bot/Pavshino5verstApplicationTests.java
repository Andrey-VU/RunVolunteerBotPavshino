package telegram.bot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class Pavshino5verstApplicationTests {
    private static TelegramBotStorage telegramBotStorage;

    private static final LocalDate eventDateToCheck = LocalDate.parse("11.11.2023", BotConfiguration.DATE_FORMATTER);
    private static final String eventRoleToCheck = "Фотограф1";
    private static final int sheetRowNumber = 6;

    User userToBeAdded;
    Participation participation;

    @BeforeAll
    static void init() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(BotConfiguration.class);
        telegramBotStorage = ctx.getBean(TelegramBotStorage.class);
    }

    @Test
    void saveUserTest() {
        var listUsersBefore = telegramBotStorage.getUsers();

        userToBeAdded = createNewUser();
        telegramBotStorage.saveUser(userToBeAdded);

        var listUsersAfter = telegramBotStorage.getUsers();
        assertThat(listUsersAfter.size() - listUsersBefore.size(), equalTo(1));

        var userIsAdded = listUsersAfter.stream().filter(user -> user.equals(userToBeAdded)).findFirst().orElse(null);
        assertThat(userIsAdded, equalTo(userToBeAdded));
    }

    @Test
    void saveParticipationTest() {
        participation = createParticipationForUser(null);
        telegramBotStorage.saveParticipation(participation);

        assertThat(isEventRoleAvailable(), equalTo(true));

        userToBeAdded = createNewUser();
        telegramBotStorage.saveUser(userToBeAdded);

        participation = createParticipationForUser(userToBeAdded);
        telegramBotStorage.saveParticipation(participation);

        assertThat(isEventRoleAvailable(), equalTo(false));
    }

    @Test
    void getParticipantsByDateTest() {
        participation = createParticipationForUser(null);
        telegramBotStorage.saveParticipation(participation);

        var listParticipationBefore = telegramBotStorage.getParticipantsByDate(eventDateToCheck);
        var counterEventRolesBusyBefore = countEventRolesBusy(listParticipationBefore);

        userToBeAdded = createNewUser();
        telegramBotStorage.saveUser(userToBeAdded);

        participation = createParticipationForUser(userToBeAdded);
        telegramBotStorage.saveParticipation(participation);

        var listParticipationAfter = telegramBotStorage.getParticipantsByDate(eventDateToCheck);
        var counterEventRolesBusyAfter = countEventRolesBusy(listParticipationAfter);
        assertThat(counterEventRolesBusyAfter - counterEventRolesBusyBefore, equalTo(1));
    }

    private User createNewUser() {
        return User.builder()
                .name("Петя")
                .surname("ИВАНОВ" + " " + LocalDateTime.now().format(DateTimeFormatter.ISO_TIME))
                .telegram("@ivanov")
                .code("0000").build();
    }

    private Participation createParticipationForUser(User user) {
        return Participation.builder()
                .user(user)
                .eventDate(eventDateToCheck)
                .eventRole(eventRoleToCheck)
                .sheetRowNumber(sheetRowNumber)
                .build();
    }

    private boolean isEventRoleAvailable() {
        return telegramBotStorage.getAvailableParticipationByDate(eventDateToCheck)
                .stream()
                .anyMatch(role -> role.getEventRole().equals(eventRoleToCheck));
    }

    private int countEventRolesBusy(List<Participation> participation) {
        return participation.stream().filter(obj -> !Objects.isNull(obj.getUser())).toList().size();
    }
}
