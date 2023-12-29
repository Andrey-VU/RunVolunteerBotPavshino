package telegram.bot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.config.BotConfiguration;
import telegram.bot.model.*;
import telegram.bot.service.enums.CallbackCommand;
import telegram.bot.service.enums.ConfirmationFeedback;
import telegram.bot.service.enums.CustomerJourneyStage;
import telegram.bot.service.factories.ReplyFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final TelegramBotStorage storage;

    /**
     * Собственно API бота
     */
    private final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

    /**
     * Токен телеграма
     */
    @Value("${telegram.token}")
    private String botToken;

    /**
     * Имя бота в телеграме
     */
    @Value("${telegram.bot_name}")
    private String botName;

    /**
     * Список активных регистраций
     */
    private final Map<Long, RegistrationForm> forms = new HashMap<>();

    /**
     * Класс формирующий ответы
     */
    private final ReplyFactory reply = new ReplyFactory();

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public TelegramBot(TelegramBotStorage storage) throws TelegramApiException {
        log.info("Bot bean created");
        this.storage = storage;
    }

    @PostConstruct
    private void init() throws TelegramApiException {
        log.info("Registering bot...");
        telegramBotsApi.registerBot(this); // Регистрируем бота
        log.info("Registration successful!!");
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("received update!");
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        if (!isKnownUser(userKeys)) {
            log.info("user unknown.");
            if (update.hasMessage() && update.getMessage().getText().equals("/start")) {
                log.info("user unknown /start command.");
                answerToUser(reply.startCommandReply(getChatId(update)));
            } else if (update.hasMessage() && update.getMessage().getText().equals("/register")) {
                log.info("user unknown /register command.");
                processCustomerJourneyStage(update);
                return;
            }
            answerToUser(reply.registrationRequired(getChatId(update)));
        } else if (update.hasMessage()) {
            if (update.getMessage().getText().startsWith("/")) {
                handleCommand(update);
            } else if (forms.containsKey(userKeys.getKey())) {
                processCustomerJourneyStage(update);
            } else {
                answerToUser(reply.commandNeededMessage(getChatId(update)));
            }
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleCommand(Update update) {
        log.info("Handling command!");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        switch (update.getMessage().getText()) {
            case "/start" -> {
                answerToUser(reply.startCommandReply(getChatId(update)));
            }
            case "/register" -> {
                if (storage.getUserByTelegram(userKeys.getValue()) != null) {
                    answerToUser(reply.alreadyRegisteredReply(chatId));
                } else {
                    processCustomerJourneyStage(update);
                }
            }
            case "/show_volunteers" -> {
                answerToUser(reply.selectDatesReply(chatId, CallbackCommand.SHOW_PART));
            }
            case "/volunteer" -> {
                answerToUser(reply.selectDatesReply(chatId, CallbackCommand.SHOW_ROLES));
            }
            case "/subscribe" -> {
                replyToSubscriptionRequester(userKeys, chatId);
            }
            default -> {
                answerToUser(reply.commandNeededMessage(chatId));
            }
        }
    }

    private void handleCallback(Update update) {
        log.info("Handling command!");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        CallbackPayload payload;
        try {
            var pr = update.getCallbackQuery().getData();
            payload = mapper.readValue(update.getCallbackQuery().getData(), CallbackPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading payload");
            return;
        }
        switch (payload.getCommand()) {
            case SHOW_PART -> {
                answerToUser(
                        reply.showVolunteersReply(
                                chatId,
                                payload.getDate(),
                                storage.getParticipantsByDate(payload.getDate())
                                        .stream()
                                        .filter(part -> part.getUser() != null)
                                        .collect(Collectors.toList())));
            }
            case SHOW_ROLES -> {
                List<Participation> participationList = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(part -> part.getUser() == null)
                        .toList();
                if (participationList.isEmpty()) {
                    answerToUser(reply.allSlotsTakenReply(chatId));
                } else {
                    answerToUser(reply.showVacantRoles(chatId, payload.getDate(), participationList));
                }
            }
            case TAKE_ROLE -> {
                // берем список участников на указанную субботу и ищем среди них нашего волонтера
                var existingUSer = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(participant -> !Objects.isNull(participant.getUser()))
                        .filter(participant -> participant.getUser().getTelegram().equals(userKeys.getValue()))
                        .findFirst().orElse(null);

                Optional.ofNullable(existingUSer).ifPresentOrElse(participant -> // если данный волонтер уже записан на какую-то роль
                                // тогда отравляем в бот сообщение об этом
                                answerToUser(reply.volunteerIsEngagedAlready(chatId, payload.getDate(), participant.getEventRole()))
                        , () -> {// если он в эту дату еще не записан на какую-либо роль

                            if (!forms.containsKey(userKeys.getKey()))
                                forms.put(userKeys.getKey(), new RegistrationForm());

                            // фиксируем, что дальше нужно будет запросить у него подтверждение записи на роль
                            forms.get(userKeys.getKey()).setStage(CustomerJourneyStage.CONFIRM_ACTION);

                            // из данных коллбэка определяем имя роли
                            var eventRole = storage.getParticipantsByDate(payload.getDate())
                                    .stream()
                                    .filter(participation -> participation.getSheetRowNumber() == payload.getSheetRowNumber())
                                    .map(Participation::getEventRole).findFirst().orElseThrow(() -> new RuntimeException("No role!"));

                            // готовим сообщение
                            String confirmationMessage = "Записать вас на " +
                                    Event.getDateLocalized(payload.getDate()) + " на роль \"" +
                                    eventRole + "\"?";

                            // отправляем диалог да/нет
                            answerToUser(
                                    reply.selectConfirmationChoice(
                                            chatId,
                                            confirmationMessage,
                                            CallbackPayload.builder()
                                                    .command(CallbackCommand.CONFIRM_PART)
                                                    .date(payload.getDate())
                                                    .sheetRowNumber(payload.getSheetRowNumber())
                                                    .build()
                                    )
                            );
                        });
            }
            case CONFIRM_REG, CONFIRM_PART -> processCustomerJourneyStage(update);
        }
    }

    private void processCustomerJourneyStage(Update update) {
        log.info("Registration progress.");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        RegistrationForm form;
        if (forms.containsKey(userKeys.getKey())) {
            form = forms.get(userKeys.getKey());
        } else {
            form = new RegistrationForm();
            answerToUser(reply.registerCommandReply(chatId));
            forms.put(userKeys.getKey(), form);
        }
        switch (form.getStage()) {
            case BEGIN -> {
                answerToUser(reply.enterNameReply(chatId));
                form.setStage(CustomerJourneyStage.ENTER_NAME);
            }
            case ENTER_NAME -> {
                form.setName(update.getMessage().getText());
                answerToUser(reply.enterSurNameReply(chatId));
                form.setStage(CustomerJourneyStage.ENTER_SURNAME);
            }
            case ENTER_SURNAME -> {
                form.setSurname(update.getMessage().getText());
                answerToUser(reply.enterCodeReply(chatId));
                form.setStage(CustomerJourneyStage.ENTER_CODE);
            }
            case ENTER_CODE -> {
                form.setCode(update.getMessage().getText());
                String confirmationMessage = "Сохранить данные: " +
                        forms.get(userKeys.getKey()).getName() + " " +
                        forms.get(userKeys.getKey()).getSurname() + ", " +
                        forms.get(userKeys.getKey()).getCode() + "?";
                answerToUser(reply.selectConfirmationChoice(chatId, confirmationMessage, CallbackPayload.builder().command(CallbackCommand.CONFIRM_REG).build()));
                form.setStage(CustomerJourneyStage.CONFIRM_ACTION);
            }
            case CONFIRM_ACTION -> {
                form.setStage(CustomerJourneyStage.BEGIN);
                CallbackPayload payload;
                try {
                    payload = mapper.readValue(update.getCallbackQuery().getData(), CallbackPayload.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                switch (CallbackCommand.valueOf(payload.getCommand().name())) {
                    case CONFIRM_REG -> {
                        switch (ConfirmationFeedback.valueOf(payload.getConfirmationAnswer())) {
                            case YES -> {
                                if (isNameAndSurnameAreCorrect(form.getName(), form.getSurname()) && isCode5VerstCorrect(form.getCode())) {
                                    User user = storage.saveUser(
                                            User.builder()
                                                    .name(form.getName())
                                                    .surname(form.getSurname())
                                                    .code(form.getCode())
                                                    .telegram(userKeys.getValue())
                                                    .comment("useless comment")
                                                    .build()
                                    );
                                    forms.remove(userKeys.getValue());
                                    answerToUser(reply.registrationDoneReply(chatId));
                                } else {
                                    forms.remove(userKeys.getValue());
                                    if (!isNameAndSurnameAreCorrect(form.getName(), form.getSurname())) {
                                        answerToUser(reply.registrationFamilyNameErrorReply(chatId));
                                    }
                                    if (!isCode5VerstCorrect(form.getCode())) {
                                        answerToUser(reply.registrationCode5VerstErrorReply(chatId));
                                    }
                                }
                            }
                            case NO -> {
                                forms.remove(userKeys.getValue());
                                answerToUser(reply.registrationCancelReply(chatId));
                            }
                        }
                    }
                    case CONFIRM_PART -> {
                        switch (ConfirmationFeedback.valueOf(payload.getConfirmationAnswer())) {
                            case YES -> {
                                // из данных коллбэка определяем имя роли
                                String eventRole = storage.getParticipantsByDate(payload.getDate())
                                        .stream()
                                        .filter(participation -> participation.getSheetRowNumber() == payload.getSheetRowNumber())
                                        .map(Participation::getEventRole).findFirst().orElseThrow(() -> new RuntimeException("No role!"));

                                // записываем информацию в таблицу
                                storage.saveParticipation(Participation.builder()
                                        .user(storage.getUserByTelegram(userKeys.getValue()))
                                        .eventDate(payload.getDate()).eventRole(eventRole).sheetRowNumber(payload.getSheetRowNumber()).build());

                                // информируем волонтера
                                answerToUser(reply.genericMessage(chatId, "Запись подтверждена"));

                                // ищем организаторов на эту дату
                                List<User> organizers = storage.getParticipantsByDate(payload.getDate())
                                        .stream()
                                        .filter(part -> part.getEventRole().equals(BotConfiguration.getSheetVolunteersRolesOrganizerName()))
                                        .map(Participation::getUser)
                                        .filter(Objects::nonNull)
                                        .toList();

                                // отправляем сообщение организаторам
                                informingOrganizers(organizers, payload.getDate(), storage.getUserByTelegram(userKeys.getValue()).getFullName(), eventRole);
                            }
                            case NO -> {
                                answerToUser(reply.genericMessage(chatId, "Запись отменена"));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isNameAndSurnameAreCorrect(String name, String surname) {
        Pattern pattern = Pattern.compile("^[a-zA-Zа-яА-ЯёЁ]+$", Pattern.UNICODE_CASE);
        return pattern.matcher(name).matches() && pattern.matcher(surname).matches();
    }

    private boolean isCode5VerstCorrect(String code) {
        Pattern codePattern = Pattern.compile("[0-9]{9}$", Pattern.UNICODE_CASE);
        return codePattern.matcher(code).matches();
    }

    private void replyToSubscriptionRequester(Map.Entry<Long, String> userKeys, long chatId) {
        storage.getUsers()
                .stream()
                .filter(user -> user.getTelegram().equals(userKeys.getValue()))
                .findAny()
                .ifPresentOrElse(user -> {
                            if (user.getIsOrganizer() && !user.getIsSubscribed()) {
                                answerToUser(reply.addOrganizerSignupReply(chatId));
                                user.setUserId(userKeys.getKey());
                                user.setIsSubscribed(true);
                                storage.updateUser(user);
                            } else if (user.getIsOrganizer()) answerToUser(reply.alreadyOrganizerSignupReply(chatId));
                            else answerToUser(reply.rejectOrganizerSignupReply(chatId));
                        }, () -> answerToUser(reply.registrationRequired(chatId))
                );
    }

    private void informingOrganizers(List<User> organizers, LocalDate eventDate, String volunteer, String eventRole) {
        organizers.stream()
                .map(User::getUserId)
                .filter(userId -> userId != 0)
                .forEach(userId -> answerToUser(reply.informOrgAboutJoinVolunteersMessage(userId, eventDate, volunteer, eventRole)));
    }

    private void answerToUser(SendMessage message) {
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("Can't send answer! - " + message.toString());
            throw new RuntimeException();
        }
    }

    private void answerToUser(long chatId, String message) {
        try {
            this.execute(reply.genericMessage(chatId, message));
        } catch (TelegramApiException e) {
            log.error("Can't send answer! - " + message);
            throw new RuntimeException();
        }
    }

    private Map.Entry<Long, String> getUserKeys(Update update) {
        if (update.hasMessage()) {
            return new AbstractMap.SimpleEntry<Long, String>(
                    update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
        } else if (update.hasCallbackQuery()) {
            return new AbstractMap.SimpleEntry<Long, String>(
                    update.getCallbackQuery().getFrom().getId(), update.getCallbackQuery().getFrom().getUserName());
        }
        throw new RuntimeException("Can't define user");
    }

    private Long getChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        throw new RuntimeException("Can't define chat");
    }

    private boolean isKnownUser(Map.Entry<Long, String> userKeys) {
        return forms.containsKey(userKeys.getKey()) || storage.getUserByTelegram(userKeys.getValue()) != null;
    }
}
