package telegram.bot.service.factories;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import telegram.bot.model.CallbackPayload;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.service.enums.CallbackCommand;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class ReplyFactory {
    private static final String GREETING_MESSAGE = "Приветствую, вас! Я бот для записи волонтёром на марафон 5 верст в парке Павшино.";
    private static final String REGISTRATION_REQUIRED_MESSAGE = "Мы с вами ещё не знакомы! Давайте я вас запишу. Введите команду /register";
    private static final String REGISTRATION_MESSAGE = "Для регистрации необходимы Имя, Фамилия,  и ваш код в Системе 5 верст";
    private static final String ALREADY_REGISTERED_MESSAGE = "Я вас уже знаю!";
    private static final String ENTER_NAME_MESSAGE = "Введите имя";
    private static final String ENTER_SURNAME_MESSAGE = "Введите фамилию";
    private static final String ENTER_5VERST_CODE_MESSAGE = "Введите код 5 верст";
    private static final String MAKE_CONFIRMATION_DATE_ROLE = "Подтвердите введённые данные!\nВы регистрируетесь на дату: ";
    private static final String REGISTRATION_DONE_MESSAGE = "Вы зарегистрированы";
    private static final String REGISTRATION_FAMILY_NAME_ERROR_MESSAGE = "Фамилия и/или имя некорректны";
    private static final String REGISTRATION_CODE_5VERST_ERROR_MESSAGE = "Введён некорректный код 5Вёрст";
    private static final String REGISTRATION_CANCEL_MESSAGE = "Регистрация отменена";
    private static final String ALL_SLOTS_TAKEN_MESSAGE = "На эту дату уже нет записи, попробуйте другую.";
    private static final String SELECT_DATES_MESSAGE = "Выберите дату";
    private static final String SELECT_ROLE_MESSAGE = "Выберите роль";
    private static final String ERROR_MESSAGE = "Что-то пошло не так";
    private static final String COMMAND_REQUIRED_MESSAGE = "Введите команду!";
    private static final String INFORM_ORG_JOIN_VOLUNTEERS_MESSAGE = " регистрируется в волонтеры на позицию ";
    private static final String ORG_ADD_SIGNUP_MESSAGE = "Вы подписаны на рассылку уведомлений о записи волонтеров";
    private static final String ORG_ALREADY_SIGNUP_MESSAGE = "Вы уже подписаны на рассылку уведомлений о записи волонтеров";
    private static final String ORG_REJECT_SIGNUP_MESSAGE = "Вас нет в списке организаторов";
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

    public SendMessage addOrganizerSignupReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ORG_ADD_SIGNUP_MESSAGE).build();
    }

    public SendMessage alreadyOrganizerSignupReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ORG_ALREADY_SIGNUP_MESSAGE).build();
    }

    public SendMessage rejectOrganizerSignupReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ORG_REJECT_SIGNUP_MESSAGE).build();
    }

    public SendMessage selectDatesReply(long chatId, CallbackCommand command) {
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

    public SendMessage makeConfirmationDateRole(long chatId, LocalDate date, String eventRole) {
        SendMessage message = SendMessage.builder().chatId(chatId).text(MAKE_CONFIRMATION_DATE_ROLE + "\"" + date.toString() + "\""
                        + "\nНа роль: \"" + eventRole + "\""
                        + "\nЕсли всё верно, выбирайте: \"YES\"!" +
                        "\nЕсли что-то пошло не так, откажитесь от сохранения: \"NO\"!")
                .replyMarkup(keyboardFactory.getApproveDeclineButtonsMarkup())
                .build();
        return message;
    }

    public SendMessage enterSurNameReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ENTER_SURNAME_MESSAGE).build();
    }

    public SendMessage enterCodeReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ENTER_5VERST_CODE_MESSAGE).build();
    }

    public SendMessage registrationDoneReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTRATION_DONE_MESSAGE).build();
    }

    public SendMessage registrationFamilyNameErrorReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTRATION_FAMILY_NAME_ERROR_MESSAGE).build();
    }

    public SendMessage registrationCode5VerstErrorReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTRATION_CODE_5VERST_ERROR_MESSAGE).build();
    }

    public SendMessage registrationCancelReply(long chatId) {
        return SendMessage.builder().chatId(chatId).text(REGISTRATION_CANCEL_MESSAGE).build();
    }

    public SendMessage showVolunteersReply(long chatId, LocalDate date, List<Participation> participationList) {
        StringBuilder builder = new StringBuilder();
        builder.append("На ").append(Event.getDateLocalized(date)).append(" записаны:\n\n");
        for (Participation participation : participationList) {
            builder.append(new String(new byte[]{(byte) 0xE2, (byte) 0x9C, (byte) 0x85}, StandardCharsets.UTF_8)).append(" ").append(participation.getEventRole()).append(" - ").append(participation.getVolunteer().getFullName()).append("\n\n");
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
        return SendMessage.builder().chatId(chatId).text("Вы записаны на " + Event.getDateLocalized(eventDate) + " на позицию " + eventRole).build();
    }

    public SendMessage volunteerIsEngagedAlready(long chatId, LocalDate eventDate, String eventRole) {
        return SendMessage.builder().chatId(chatId).text(new String(new byte[]{(byte) 0xE2, (byte) 0x9D, (byte) 0x97}, StandardCharsets.UTF_8) + " " + Event.getDateLocalized(eventDate) + " вы уже записаны на роль " + eventRole).build();
    }

    public SendMessage errorMessage(long chatId) {
        return SendMessage.builder().chatId(chatId).text(ERROR_MESSAGE).build();
    }

    public SendMessage commandNeededMessage(long chatId) {
        return SendMessage.builder().chatId(chatId).text(COMMAND_REQUIRED_MESSAGE).build();
    }

    public SendMessage selectConfirmationChoice(long chatId, String message, CallbackPayload callbackPayload) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .replyMarkup(keyboardFactory.getConfirmationButtons(callbackPayload))
                .build();
    }

    public SendMessage informOrgAboutJoinVolunteersMessage(long chatId, LocalDate date, String volunteer, String eventRole) {
        return SendMessage.builder().chatId(chatId).text(volunteer + INFORM_ORG_JOIN_VOLUNTEERS_MESSAGE + "\"" + eventRole + "\" на дату " + date + "!").build();
    }

    public SendMessage genericMessage(long chatId, String message) {
        return SendMessage.builder().chatId(chatId).text(message).build();
    }
}
