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
import telegram.bot.model.Participation;
import telegram.bot.model.User;
import telegram.bot.service.enums.Callbackcommands;
import telegram.bot.service.enums.RegistrationStages;
import telegram.bot.service.factories.ReplyFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("recieved update!");
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        if(!isKnownUser(userKeys)) {
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
        } else if(update.hasMessage()) {
            if (update.getMessage().getText().startsWith("/")) {
                handleCommand(update);
            } else if (forms.containsKey(userKeys.getKey())) {
                registration(update);
            } else {
                answerToUser(reply.commandNeededMessage(getChatId(update)));
            }
        } else if(update.hasCallbackQuery()) {
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
        log.info("Hadling command!");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        switch (update.getMessage().getText()) {
            case "/start" -> {
                answerToUser(reply.startCommandReply(getChatId(update)));
            }
            case "/register" -> {
                if(storage.getUserByTelegram(userKeys.getValue()) != null) {
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
            default -> {
                answerToUser(reply.commandNeededMessage(chatId));
            }
        }
    }

    private void handleCallback(Update update) {
        log.info("Handling command!");
        long chatId = getChatId(update);
        Map.Entry<Long, String> userKeys = getUserKeys(update);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        CallbackPayload payload;
        try {
            var pr = update.getCallbackQuery().getData();
             payload = mapper.readValue(update.getCallbackQuery().getData(), CallbackPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading payload");
            throw new RuntimeException();
            //return;
        }
        switch (payload.getCommand()) {
            case SHOW -> {
                answerToUser(
                        reply.showVolunTeersReply(
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
                // Вот тут из-за общей модели проблема
                int rowNum = storage.getParticipantsByDate(payload.getDate())
                                .stream()
                        .filter(participation -> participation.getEventRole().equals(payload.getRole()))
                                .findFirst().orElseThrow(() -> new RuntimeException("No role!")).getSheetRowNumber();
                storage.saveParticipation(Participation.builder()
                        .user(storage.getUserByTelegram(userKeys.getValue()))
                        .eventDate(payload.getDate()).eventRole(payload.getRole()).sheetRowNumber(rowNum).build());
            }
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
            }
        }
    }

}
