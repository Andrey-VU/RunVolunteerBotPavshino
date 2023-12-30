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
import telegram.bot.service.enums.CallbackCommandType;
import telegram.bot.service.enums.UserActionType;
import telegram.bot.service.enums.UserChoiceType;
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
    private final Map<Long, UserRecord> userRecords = new HashMap<>();

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
        log.info("onUpdateReceived");
        Map.Entry<Long, String> userIdentity = getUserIdentity(update);

        // если этого юзера еще нет в мапе юзеров бота, добавляем его туда и отправляем приветствие
        if (!userRecords.containsKey(userIdentity.getKey())) {
            userRecords.put(userIdentity.getKey(), UserRecord.builder().expectedUserActionType(UserActionType.CHOOSE_COMMAND).build());
            answerToUser(reply.botGreetingReply(getChatId(update)));
        }

        if (update.hasMessage()) {
            if (update.getMessage().getText().startsWith("/"))
                handleCommand(update); // если пользователь выбрал команду из меню
            else
                handleStage(update); // если пользователь что-то написал
        } else if (update.hasCallbackQuery()) {
            handleCallback(update); // если пользователь кликнул кнопку
        }
    }

    private void handleCommand(Update update) {
        log.info("handleCommand");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userIdentity = getUserIdentity(update);
        UserRecord userRecord = userRecords.get(userIdentity.getKey());

        switch (update.getMessage().getText()) {
            case "/start" -> {
                userRecord.setExpectedUserActionType(UserActionType.CHOOSE_COMMAND);
                answerToUser(reply.botGreetingReply(getChatId(update)));
            }
            case "/register" -> {
                if (Objects.nonNull(storage.getVolunteerByTelegram(userIdentity.getValue()))) {
                    userRecord.setExpectedUserActionType(UserActionType.ENTER_NAME);
                    answerToUser(reply.alreadyRegisteredReply(chatId));
                } else handleStage(update);
            }
            case "/show_volunteers" -> {
                userRecord.setExpectedUserActionType(UserActionType.CLICK_BUTTON);
                answerToUser(reply.selectDatesReply(chatId, CallbackCommandType.SHOW_PART));
            }
            case "/volunteer" -> {
                if (Objects.isNull(storage.getVolunteerByTelegram(userIdentity.getValue()))) {
                    userRecord.setExpectedUserActionType(UserActionType.CHOOSE_COMMAND);
                    answerToUser(reply.registrationRequired(getChatId(update)));
                } else {
                    userRecord.setExpectedUserActionType(UserActionType.CLICK_BUTTON);
                    answerToUser(reply.selectDatesReply(chatId, CallbackCommandType.SHOW_ROLES));
                }
            }
            case "/subscribe" -> {
                userRecord.setExpectedUserActionType(UserActionType.CHOOSE_COMMAND);
                if (Objects.isNull(storage.getVolunteerByTelegram(userIdentity.getValue())))
                    answerToUser(reply.registrationRequired(getChatId(update)));
                else replyToSubscriptionRequester(userIdentity, chatId);
            }
            default -> {
                userRecord.setExpectedUserActionType(UserActionType.CHOOSE_COMMAND);
                answerToUser(reply.genericMessage(chatId, "Выберите команду из меню"));
            }
        }
    }

    private void handleCallback(Update update) {
        log.info("handleCallback");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userIdentity = getUserIdentity(update);
        UserRecord userRecord = userRecords.get(userIdentity.getKey());

//        if (volunteerBotRecords.containsKey(userIdentity.getKey()) && volunteerBotRecords.get(userIdentity.getKey()).getStage() == TgUserJourneyStage.INITIAL) {
//            answerToUser(reply.genericMessage(chatId, "Выберите команду из меню"));
//            return;
//        }

        CallbackPayload payload;
        try {
            var pr = update.getCallbackQuery().getData();
            payload = mapper.readValue(update.getCallbackQuery().getData(), CallbackPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading payload");
            return;
        }
        switch (payload.getCallbackCommandType()) {
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
                List<Participation> vacantRolesList = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(part -> part.getVolunteer() == null)
                        .toList();
                if (vacantRolesList.isEmpty()) {
                    answerToUser(reply.allSlotsTakenReply(chatId));
                } else {
                    answerToUser(reply.showVacantRoles(chatId, payload.getDate(), vacantRolesList));
                }
            }
            case TAKE_ROLE -> {
                // берем список участников на указанную субботу и ищем среди них нашего волонтера
                var existingUSer = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(participant -> !Objects.isNull(participant.getVolunteer()))
                        .filter(participant -> participant.getVolunteer().getTgUserName().equals(userIdentity.getValue()))
                        .findFirst().orElse(null);

                Optional.ofNullable(existingUSer).ifPresentOrElse(participant -> // если волонтер уже записан на какую-то роль
                                // тогда отравляем в бот сообщение об этом
                                answerToUser(reply.volunteerIsEngagedAlready(chatId, payload.getDate(), participant.getEventRole()))
                        , () -> {
                            // если волонтер в эту дату еще не записан на какую-либо роль

                            // из данных полученного коллбэка определяем имя роли, которую выбрад волонтер
                            var eventRole = storage.getParticipantsByDate(payload.getDate())
                                    .stream()
                                    .filter(participation -> participation.getSheetRowNumber() == payload.getSheetRowNumber())
                                    .map(Participation::getEventRole).findFirst().orElseThrow(() -> new RuntimeException("No role!"));

                            // готовим сообщение для диалога подтверждения
                            String confirmationMessage = "Записать вас на " +
                                    Event.getDateLocalized(payload.getDate()) + " на роль \"" +
                                    eventRole + "\"?";

                            // готовим коллбэк для диалога подтверждения
                            var callBack = CallbackPayload.builder()
                                    .callbackCommandType(CallbackCommandType.CONFIRM_PART)
                                    .date(payload.getDate())
                                    .sheetRowNumber(payload.getSheetRowNumber())
                                    .build();

                            // запоминаем, что дальше нужно будет запросить у него подтверждение записи на роль
                            userRecords.get(userIdentity.getKey()).setExpectedUserActionType(UserActionType.CONFIRM_CHOICE);

                            // отправляем диалог да/нет
                            answerToUser(reply.selectConfirmationChoice(chatId, confirmationMessage, callBack));
                        });
            }
            case CONFIRM_REG, CONFIRM_PART -> handleStage(update);
        }
    }

    private void handleStage(Update update) {
        log.info("handleStage");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userIdentity = getUserIdentity(update);
        UserRecord userRecord = userRecords.get(userIdentity.getKey());

        switch (userRecord.getExpectedUserActionType()) {
            case CHOOSE_COMMAND -> answerToUser(reply.genericMessage(chatId, "Выберите команду из меню"));
            case REGISTRATION_REQUIRED -> {
                userRecord.setExpectedUserActionType(UserActionType.ENTER_NAME);
                answerToUser(reply.registerInitialReply(chatId));
                answerToUser(reply.enterNameReply(chatId));
            }
            case ENTER_NAME -> {
                userRecord.setName(update.getMessage().getText());

                userRecord.setExpectedUserActionType(UserActionType.ENTER_SURNAME);
                answerToUser(reply.enterSurNameReply(chatId));
            }
            case ENTER_SURNAME -> {
                userRecord.setSurname(update.getMessage().getText());

                userRecord.setExpectedUserActionType(UserActionType.ENTER_CODE);
                answerToUser(reply.enterCodeReply(chatId));
            }
            case ENTER_CODE -> {
                userRecord.setCode(update.getMessage().getText());

                String confirmationMessage = "Зарегистрировать вас в списке волонтеров: " +
                        userRecords.get(userIdentity.getKey()).getName() + " " +
                        userRecords.get(userIdentity.getKey()).getSurname() + ", " +
                        userRecords.get(userIdentity.getKey()).getCode() + "?";
                userRecord.setExpectedUserActionType(UserActionType.CONFIRM_CHOICE);
                answerToUser(reply.selectConfirmationChoice(chatId, confirmationMessage, CallbackPayload.builder().callbackCommandType(CallbackCommandType.CONFIRM_REG).build()));
            }
            case CONFIRM_CHOICE -> {
                CallbackPayload payload;
                try {
                    payload = mapper.readValue(update.getCallbackQuery().getData(), CallbackPayload.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                switch (CallbackCommandType.valueOf(payload.getCallbackCommandType().name())) {
                    case CONFIRM_REG -> {
                        switch (UserChoiceType.valueOf(payload.getConfirmationAnswer())) {
                            case YES -> {
                                if (storage.getVolunteerByTelegram(userIdentity.getValue()) != null)
                                    answerToUser(reply.alreadyRegisteredReply(chatId));
                                else {
                                    if (isNameAndSurnameAreCorrect(userRecord.getName(), userRecord.getSurname()) && isCode5VerstCorrect(userRecord.getCode())) {
                                        Volunteer volunteer = storage.saveVolunteer(
                                                Volunteer.builder()
                                                        .name(userRecord.getName())
                                                        .surname(userRecord.getSurname())
                                                        .code(userRecord.getCode())
                                                        .tgUserName(userIdentity.getValue())
                                                        .tgUserId(userIdentity.getKey())
                                                        .comment("useless comment")
                                                        .build()
                                        );
                                        answerToUser(reply.registrationDoneReply(chatId));
                                    } else {
                                        if (!isNameAndSurnameAreCorrect(userRecord.getName(), userRecord.getSurname())) {
                                            answerToUser(reply.registrationFamilyNameErrorReply(chatId));
                                        }
                                        if (!isCode5VerstCorrect(userRecord.getCode())) {
                                            answerToUser(reply.registrationCode5VerstErrorReply(chatId));
                                        }
                                    }
                                }
                            }
                            case NO -> {
                                if (storage.getVolunteerByTelegram(userIdentity.getValue()) != null)
                                    answerToUser(reply.genericMessage(chatId, "Вы уже зарегистрированы"));
                                else answerToUser(reply.registrationCancelReply(chatId));
                            }
                        }
                    }
                    case CONFIRM_PART -> {
                        switch (UserChoiceType.valueOf(payload.getConfirmationAnswer())) {
                            case YES -> {
                                // из данных коллбэка определяем имя роли
                                String eventRole = storage.getParticipantsByDate(payload.getDate())
                                        .stream()
                                        .filter(participation -> participation.getSheetRowNumber() == payload.getSheetRowNumber())
                                        .map(Participation::getEventRole).findFirst().orElseThrow(() -> new RuntimeException("No role!"));

                                // записываем информацию в таблицу
                                storage.saveParticipation(Participation.builder()
                                        .volunteer(storage.getVolunteerByTelegram(userIdentity.getValue()))
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
                                informingOrganizers(organizers, payload.getDate(), storage.getVolunteerByTelegram(userIdentity.getValue()).getFullName(), eventRole);
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
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Can't send answer! - " + message.toString());
            throw new RuntimeException();
        }
    }

    private void answerToUser(long chatId, String message) {
        try {
            execute(reply.genericMessage(chatId, message));
        } catch (TelegramApiException e) {
            log.error("Can't send answer! - " + message);
            throw new RuntimeException();
        }
    }

    private Map.Entry<Long, String> getUserIdentity(Update update) {
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
}
