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
import telegram.bot.model.Participation;
import telegram.bot.model.User;
import telegram.bot.service.enums.Callbackcommands;
import telegram.bot.service.enums.ConfirmationFeedback;
import telegram.bot.service.enums.RegistrationStages;
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
        log.info("Registerung bot...");
        telegramBotsApi.registerBot(this); // Регистрируем бота
        log.info("Registrartion successfull!!");
        mapper.registerModule(new JavaTimeModule());
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
                registration(update);

                return;
            }
            answerToUser(reply.registrationRequired(getChatId(update)));
        } else if (update.hasMessage()) {
            if (update.getMessage().getText().startsWith("/")) {
                handleCommand(update);
            } else if (forms.containsKey(userKeys.getKey())) {
                registration(update);
            } else {
                answerToUser(reply.commandNeededMessage(getChatId(update)));
            }
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private void answerToUser(SendMessage message) {
        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("Can't send answer! - " + message.toString());
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
                    registration(update);
                }
            }
            case "/show_volunteers" -> {
                answerToUser(reply.selectDatesReply(chatId, Callbackcommands.SHOW));
            }
            case "/volunteer" -> {
                answerToUser(reply.selectDatesReply(chatId, Callbackcommands.VOLUNTEER));
            }
            case "/subscribe" -> {
                replyToSubscriptionRequestor(userKeys, chatId);
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
            case SHOW -> {
                answerToUser(
                        reply.showVolunteersReply(
                                chatId,
                                payload.getDate(),
                                storage.getParticipantsByDate(payload.getDate())
                                        .stream()
                                        .filter(part -> part.getUser() != null)
                                        .collect(Collectors.toList())));
            }
            case VOLUNTEER -> {
                List<Participation> participations = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(part -> part.getUser() == null)
                        .toList();
                if (participations.isEmpty()) {
                    answerToUser(reply.allSlotsTakenReply(chatId));
                } else {
                    answerToUser(reply.showVacantRoles(chatId, payload.getDate(), participations));
                }
            }
            case ROLE -> {
                List<User> organizers = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(part -> part.getEventRole().equals(BotConfiguration.getSheetVolunteersRolesOrganizerName())) // ищем организаторов на эту дату
                        .map(Participation::getUser)
                        .toList();

                String eventRole = storage.getParticipantsByDate(payload.getDate())
                        .stream()
                        .filter(participation -> participation.getSheetRowNumber() == payload.getSheetRowNumber())
                        .map(Participation::getEventRole).findFirst().orElseThrow(() -> new RuntimeException("No role!"));


                var existingUSer = storage.getParticipantsByDate(payload.getDate()) // берем список участников на указанную субботу
                        .stream()
                        .filter(participant -> !Objects.isNull(participant.getUser()))
                        .filter(participant -> participant.getUser().getTelegram().equals(userKeys.getValue())) // ищем нашего волонтера
                        .findFirst().orElse(null);

                Optional.ofNullable(existingUSer).ifPresentOrElse(participant -> // если данный волонтер уже записан на какую роль
                                answerToUser(reply.volunteerIsEngagedAlready(chatId, payload.getDate(), participant.getEventRole())) // тогда отравляем в бот сообщение об этом
                        , () -> { // иначе записываем его на запрошенную роль
                            storage.saveParticipation(Participation.builder() // записываем в файл
                                    .user(storage.getUserByTelegram(userKeys.getValue()))
                                    .eventDate(payload.getDate()).eventRole(eventRole).sheetRowNumber(payload.getSheetRowNumber()).build());
                            answerToUser(reply.roleReservationDoneReply(chatId, payload.getDate(), eventRole)); // отправляем в бот сообщение об этом

                            informingOrganizers(organizers, payload.getDate(), storage.getUserByTelegram(userKeys.getValue()).getFullName(), eventRole); // отправляем сообщение организаторам
                        });
            }
            case CONFIRMATION -> registration(update);
        }
    }

    private void registration(Update update) {
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
            case NEW -> {
                answerToUser(reply.enterNameReply(chatId));
                form.setStage(RegistrationStages.NAME);
            }
            case NAME -> {
                form.setName(update.getMessage().getText());
                answerToUser(reply.enterSurNameReply(chatId));
                form.setStage(RegistrationStages.SURNAME);
            }
            case SURNAME -> {
                form.setSurname(update.getMessage().getText());
                answerToUser(reply.enterCodeReply(chatId));
                form.setStage(RegistrationStages.CODE);
            }
            case CODE -> {
                form.setCode(update.getMessage().getText());
                String confirmationMessage = "Сохранить данные: " +
                        forms.get(userKeys.getKey()).getName() + " " +
                        forms.get(userKeys.getKey()).getSurname() + ", " +
                        forms.get(userKeys.getKey()).getCode() + "?";
                answerToUser(reply.selectConfirmationChoice(chatId, confirmationMessage));
                form.setStage(RegistrationStages.CONFIRMATION);
            }
            case CONFIRMATION -> {
                form.setStage(RegistrationStages.NEW);
                CallbackPayload payload;
                try {
                    payload = mapper.readValue(update.getCallbackQuery().getData(), CallbackPayload.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                switch (ConfirmationFeedback.valueOf(payload.getConfirmationAnswer())) {
                    case YES -> {
                        if (isNameAndSurnameAreCorrect(form.getName(), form.getSurname())) {
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
                            answerToUser(reply.registrationErrorReply(chatId));
                        }
                    }
                    case NO -> {
                        forms.remove(userKeys.getValue());
                        answerToUser(reply.registrationCancelReply(chatId));
                    }
                }
            }
        }
    }

    private boolean isNameAndSurnameAreCorrect(String name, String surname) {
        Pattern pattern = Pattern.compile("^[a-zA-Zа-яА-ЯёЁ]+$", Pattern.UNICODE_CASE);
        return pattern.matcher(name).matches() && pattern.matcher(surname).matches();
    }

    private void replyToSubscriptionRequestor(Map.Entry<Long, String> userKeys, long chatId) {
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
}
