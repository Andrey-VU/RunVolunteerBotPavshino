package telegram.bot.service.factories;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import telegram.bot.model.Participation;
import telegram.bot.service.enums.Callbackcommands;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReplyFactory {

    private static final String GREETING_MESSAGE = "Приветствую, вас! Я бот для записи волонтёром на марафон" +
            " 5 верст в парке Павшино.";

    private static final String REGISTRATION_REQUIRED_MESSAGE = "Мы с вами ещё не знакомы! Давайте я вас запишу." +
            "Введите комманду /register";

    private static final String REGISTRATION_MESSAGE = "Для регистрации необходимы Фамилиля, Имя и ваш код " +
            "в Системе 5 верст";

    private static final String ALREADY_REGISTERED_MESSAGE = "Я вас уже знаю!";

    private static final String ENTER_NAME_MESSAGE = "Введите имя";

    private static final String ENTER_SURNAME_MESSAGE = "Введите фамилию";

    private static final String ENTER_5VERST_CODE_MESSAGE = "Введите код 5 верст";

    private static final String REGISTERATION_DONE_MESSAGE = "Вы зарегистрированы";

    private static final String VOLUNTEER_MESSAGE = "Вы хотите записаться, на участие в марафоне.";

    private static final String ALL_SLOTS_TAKEN_MESSAGE = "На эту дату все уже нет записей, попробуйте другую.";

    private static final String SELECT_DATES_MESSAGE = "Выберите дату";

    private static final String SELECT_ROLE_MESSAGE = "Выберите роль";

    private static final String SHOW_PARTICIPANTS_MESSAGE = "На какую дату вас интересует список участников";

    // public static final String PARTICIPATION_SUCCESSFULL_MESSAGE = "В";

    private static final String ERROR_MESSAGE = "Что-то пошло не так.";

    private static final String COMMAND_REQUIRED_MESSAGE = "Введите комманду!";

    private final KeyboardFactory keyboardFactory = new KeyboardFactory();

    public SendMessage startCommandReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(GREETING_MESSAGE).build();
    }

    public SendMessage registerCommandReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTRATION_MESSAGE).build();
    }

    public SendMessage alreadyRegisteredReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ALREADY_REGISTERED_MESSAGE).build();
    }

    public SendMessage selectDatesReply(long chatId, Callbackcommands command) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(SELECT_DATES_MESSAGE)
                .replyMarkup(keyboardFactory.getFourDatesMarkup(command))
                .build();
    }

    public SendMessage registrationRequired(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTRATION_REQUIRED_MESSAGE).build();
    }

    public SendMessage enterNameReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ENTER_NAME_MESSAGE).build();
    }

    public SendMessage enterSurNameReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ENTER_SURNAME_MESSAGE).build();
    }

    public SendMessage enterCodeReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ENTER_5VERST_CODE_MESSAGE).build();
    }

    public SendMessage registrationDoneReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTERATION_DONE_MESSAGE).build();
    }

    public SendMessage showVolunTeersReply(long chatId, LocalDate date, List<Participation> participats) {
        StringBuilder builder = new StringBuilder();
        builder.append("На ").append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(" записаны:\n");
        for (Participation part : participats) {
            builder.append(part.getEventRole() + " - ").append(part.getUser().getFullName() + "\n");
        }
        return SendMessage.builder().chatId(chatId)
                .text(builder.toString()).build();
    }

    public SendMessage allSlotsTakenReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ALL_SLOTS_TAKEN_MESSAGE).build();
    }

    public SendMessage showVacantRoles(long chatId, LocalDate date, List<Participation> participats) {
        return SendMessage.builder().chatId(chatId).text(SELECT_ROLE_MESSAGE)
                .replyMarkup(keyboardFactory.getVacantRolesMarkup(date, participats)).build();
    }

    public SendMessage roleReservationDoneReply(long chatId, LocalDate eventDate, String eventRole) {
        return SendMessage.builder().chatId(chatId).text("Вы записаны на " + eventDate.format(DateTimeFormatter.ISO_DATE) + " на позицию " + eventRole).build();
    }

    public SendMessage errorMessage(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ERROR_MESSAGE).build();
    }

    public SendMessage commandNeededMessage(long chatId) {
        return SendMessage.builder().chatId(chatId).text(COMMAND_REQUIRED_MESSAGE).build();
    }
}
