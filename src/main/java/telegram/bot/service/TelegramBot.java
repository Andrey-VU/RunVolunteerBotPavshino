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
import telegram.bot.service.enums.TgUserJourneyStage;
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
    private final Map<Long, VolunteerBotRecord> volunteerBotRecords = new HashMap<>();

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
            } else if (volunteerBotRecords.containsKey(userKeys.getKey())) {
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
                if (storage.getVolunteerByTelegram(userKeys.getValue()) != null) {
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
                                        .filter(part -> part.getVolunteer() != null)
                                        .collect(Collectors.toList())));
            }
            case SHOW_ROLES -> {
                List<Participation> participationList = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(part -> part.getVolunteer() == null)
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
                        .filter(participant -> !Objects.isNull(participant.getVolunteer()))
                        .filter(participant -> participant.getVolunteer().getTgUserName().equals(userKeys.getValue()))
                        .findFirst().orElse(null);

                Optional.ofNullable(existingUSer).ifPresentOrElse(participant -> // если данный волонтер уже записан на какую-то роль
                                // тогда отравляем в бот сообщение об этом
                                answerToUser(reply.volunteerIsEngagedAlready(chatId, payload.getDate(), participant.getEventRole()))
                        , () -> {// если он в эту дату еще не записан на какую-либо роль

                            if (!volunteerBotRecords.containsKey(userKeys.getKey()))
                                volunteerBotRecords.put(userKeys.getKey(), new VolunteerBotRecord());

                            // фиксируем, что дальше нужно будет запросить у него подтверждение записи на роль
                            volunteerBotRecords.get(userKeys.getKey()).setStage(TgUserJourneyStage.CONFIRM_ACTION);

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
        log.info("processCustomerJourneyStage");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        VolunteerBotRecord volunteerBotRecord;
        if (this.volunteerBotRecords.containsKey(userKeys.getKey())) {
            volunteerBotRecord = this.volunteerBotRecords.get(userKeys.getKey());
        } else {
            volunteerBotRecord = new VolunteerBotRecord();
            answerToUser(reply.registerCommandReply(chatId));
            this.volunteerBotRecords.put(userKeys.getKey(), volunteerBotRecord);
        }
        switch (volunteerBotRecord.getStage()) {
            case BEGIN -> {
                answerToUser(reply.enterNameReply(chatId));
                volunteerBotRecord.setStage(TgUserJourneyStage.ENTER_NAME);
            }
            case ENTER_NAME -> {
                volunteerBotRecord.setName(update.getMessage().getText());
                answerToUser(reply.enterSurNameReply(chatId));
                volunteerBotRecord.setStage(TgUserJourneyStage.ENTER_SURNAME);
            }
            case ENTER_SURNAME -> {
                volunteerBotRecord.setSurname(update.getMessage().getText());
                answerToUser(reply.enterCodeReply(chatId));
                volunteerBotRecord.setStage(TgUserJourneyStage.ENTER_CODE);
            }
            case ENTER_CODE -> {
                volunteerBotRecord.setCode(update.getMessage().getText());
                String confirmationMessage = "Сохранить данные: " +
                        this.volunteerBotRecords.get(userKeys.getKey()).getName() + " " +
                        this.volunteerBotRecords.get(userKeys.getKey()).getSurname() + ", " +
                        this.volunteerBotRecords.get(userKeys.getKey()).getCode() + "?";
                answerToUser(reply.selectConfirmationChoice(chatId, confirmationMessage, CallbackPayload.builder().command(CallbackCommand.CONFIRM_REG).build()));
                volunteerBotRecord.setStage(TgUserJourneyStage.CONFIRM_ACTION);
            }
            case CONFIRM_ACTION -> {
                volunteerBotRecord.setStage(TgUserJourneyStage.BEGIN);
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
                                if (isNameAndSurnameAreCorrect(volunteerBotRecord.getName(), volunteerBotRecord.getSurname()) && isCode5VerstCorrect(volunteerBotRecord.getCode())) {
                                    Volunteer volunteer = storage.saveVolunteer(
                                            Volunteer.builder()
                                                    .name(volunteerBotRecord.getName())
                                                    .surname(volunteerBotRecord.getSurname())
                                                    .code(volunteerBotRecord.getCode())
                                                    .tgUserName(userKeys.getValue())
                                                    .comment("useless comment")
                                                    .build()
                                    );
                                    this.volunteerBotRecords.remove(userKeys.getValue());
                                    answerToUser(reply.registrationDoneReply(chatId));
                                } else {
                                    this.volunteerBotRecords.remove(userKeys.getValue());
                                    if (!isNameAndSurnameAreCorrect(volunteerBotRecord.getName(), volunteerBotRecord.getSurname())) {
                                        answerToUser(reply.registrationFamilyNameErrorReply(chatId));
                                    }
                                    if (!isCode5VerstCorrect(volunteerBotRecord.getCode())) {
                                        answerToUser(reply.registrationCode5VerstErrorReply(chatId));
                                    }
                                }
                            }
                            case NO -> {
                                this.volunteerBotRecords.remove(userKeys.getValue());
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
                                        .volunteer(storage.getVolunteerByTelegram(userKeys.getValue()))
                                        .eventDate(payload.getDate()).eventRole(eventRole).sheetRowNumber(payload.getSheetRowNumber()).build());

                                // информируем волонтера
                                answerToUser(reply.genericMessage(chatId, "Запись подтверждена"));

                                // ищем организаторов на эту дату
                                List<Volunteer> organizers = storage.getParticipantsByDate(payload.getDate())
                                        .stream()
                                        .filter(part -> part.getEventRole().equals(BotConfiguration.getSheetVolunteersRolesOrganizerName()))
                                        .map(Participation::getVolunteer)
                                        .filter(Objects::nonNull)
                                        .toList();

                                // отправляем сообщение организаторам
                                informingOrganizers(organizers, payload.getDate(), storage.getVolunteerByTelegram(userKeys.getValue()).getFullName(), eventRole);
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
        storage.getVolunteers()
                .stream()
                .filter(user -> user.getTgUserName().equals(userKeys.getValue()))
                .findAny()
                .ifPresentOrElse(user -> {
                            if (user.getIsOrganizer() && !user.getIsSubscribed()) {
                                answerToUser(reply.addOrganizerSignupReply(chatId));
                                user.setTgUserId(userKeys.getKey());
                                user.setIsSubscribed(true);
                                storage.updateVolunteer(user);
                            } else if (user.getIsOrganizer()) answerToUser(reply.alreadyOrganizerSignupReply(chatId));
                            else answerToUser(reply.rejectOrganizerSignupReply(chatId));
                        }, () -> answerToUser(reply.registrationRequired(chatId))
                );
    }

    private void informingOrganizers(List<Volunteer> organizers, LocalDate eventDate, String volunteer, String eventRole) {
        organizers.stream()
                .map(Volunteer::getTgUserId)
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
        return volunteerBotRecords.containsKey(userKeys.getKey()) || storage.getVolunteerByTelegram(userKeys.getValue()) != null;
    }
}
