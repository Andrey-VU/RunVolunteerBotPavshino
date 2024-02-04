package telegram.bot.service.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import telegram.bot.model.CallbackPayload;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.service.enums.ButtonType;
import telegram.bot.service.enums.UserChoiceType;
import telegram.bot.service.utils.DatesCalculator;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {
    ObjectMapper mapper;

    public KeyboardFactory() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public InlineKeyboardMarkup getFourDatesMarkup(ButtonType buttonType) {
        List<LocalDate> dates = DatesCalculator.getNextEventDates();
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(getDateButton(dates.get(0), buttonType), getDateButton(dates.get(1), buttonType)),
                List.of(getDateButton(dates.get(2), buttonType), getDateButton(dates.get(3), buttonType))
        )).build();
    }

    public InlineKeyboardMarkup getVacantRolesMarkup(LocalDate date, List<Participation> participations) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(
                participations.stream()
                        .map(participation -> List.of(getRoleButton(date, participation)))
                        .toList());
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getConfirmationButtons(CallbackPayload callbackPayload) {
        try {
            return InlineKeyboardMarkup.builder()
                    .keyboard(List.of(
                            List.of(InlineKeyboardButton.builder()
                                            .text(new String(new byte[]{(byte) 0xE2, (byte) 0x9C, (byte) 0x85}, StandardCharsets.UTF_8) + " " + UserChoiceType.YES.name())
                                            .callbackData(mapper.writeValueAsString(getCallbackPayload(callbackPayload, UserChoiceType.YES)))
                                            .build(),
                                    InlineKeyboardButton.builder()
                                            .text(new String(new byte[]{(byte) 0xE2, (byte) 0x9D, (byte) 0x8C}, StandardCharsets.UTF_8) + " " + UserChoiceType.NO.name())
                                            .callbackData(mapper.writeValueAsString(getCallbackPayload(callbackPayload, UserChoiceType.NO)))
                                            .build()))
                    ).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CallbackPayload getCallbackPayload(CallbackPayload callbackPayload, UserChoiceType userChoiceType) {
        return CallbackPayload.builder()
                .buttonType(callbackPayload.getButtonType())
                .date(callbackPayload.getDate())
                .sheetRowNumber(callbackPayload.getSheetRowNumber())
                .userChoice(userChoiceType.name()).build();
    }

    public InlineKeyboardMarkup getApproveDeclineButtonsMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getApproveDeclineButtons());

        return inlineKeyboardMarkup;
    }

    public ReplyKeyboardMarkup getMainMenu() {
        var keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(ReplyFactory.COMMAND_TAKE_PARTICIPATION);
        keyboardRow1.add(ReplyFactory.COMMAND_SHOW_VOLUNTEERS);
        var keyboardRow2 = new KeyboardRow();
        keyboardRow2.add(ReplyFactory.COMMAND_VOLUNTEER_REGISTRATION);
        keyboardRow2.add(ReplyFactory.COMMAND_SUBSCRIBE_NOTIFICATION);
        var keyboardRow3 = new KeyboardRow();
        keyboardRow3.add(ReplyFactory.COMMAND_HELP);
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(keyboardRow1, keyboardRow2, keyboardRow3))
                .resizeKeyboard(true)
                .build();
    }

    private List<List<InlineKeyboardButton>> getApproveDeclineButtons() {

        List<List<InlineKeyboardButton>> approveDeclineButtons = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        try {
            InlineKeyboardButton approveButton = new InlineKeyboardButton("YES!");
            approveButton.setCallbackData("YES");
            InlineKeyboardButton declineButton = new InlineKeyboardButton("NO!");
            declineButton.setCallbackData("NO");

            buttons.add(approveButton);
            buttons.add(declineButton);

            approveDeclineButtons.add(buttons);

        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return approveDeclineButtons;
    }

    private InlineKeyboardButton getRoleButton(LocalDate date, Participation participation) {
        CallbackPayload payload = CallbackPayload.builder()
                .date(date)
                .sheetRowNumber(participation.getSheetRowNumber())
                .buttonType(
                        !participation.isPointerToNextPageOfRoles() ?
                        ButtonType.CHOSEN_ROLE :
                        ButtonType.TAKE_PART2)
                .build();
        try {
            return InlineKeyboardButton.builder()
                    .text((!participation.isPointerToNextPageOfRoles() ?
                            new String(new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0xA9}, StandardCharsets.UTF_8) : "")
                            + " " + participation.getEventRole()).callbackData(mapper.writeValueAsString(payload)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InlineKeyboardButton getDateButton(LocalDate date, ButtonType buttonType) {
        CallbackPayload payload = CallbackPayload.builder().date(date).buttonType(buttonType).build();
        try {
            var caption = new String(new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x93, (byte) 0x85}, StandardCharsets.UTF_8) + " " + Event.getDateLocalized(date);
            return InlineKeyboardButton.builder()
                    .text(caption)
                    .callbackData(mapper.writeValueAsString(payload)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
