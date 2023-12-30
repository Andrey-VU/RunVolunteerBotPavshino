package telegram.bot.service.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.bot.model.CallbackPayload;
import telegram.bot.model.Event;
import telegram.bot.model.Participation;
import telegram.bot.service.enums.CallbackCommand;
import telegram.bot.service.enums.ConfirmationFeedback;
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

    public InlineKeyboardMarkup getFourDatesMarkup(CallbackCommand command) {
        List<LocalDate> dates = DatesCalculator.getNextEventDates();
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(getDateButton(dates.get(0), command), getDateButton(dates.get(1), command)),
                List.of(getDateButton(dates.get(2), command), getDateButton(dates.get(3), command))
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
                                            .text(new String(new byte[]{(byte) 0xE2, (byte) 0x9C, (byte) 0x85}, StandardCharsets.UTF_8) + " " + ConfirmationFeedback.YES.name())
                                            .callbackData(mapper.writeValueAsString(getCallbackPayload(callbackPayload, ConfirmationFeedback.YES)))
                                            .build(),
                                    InlineKeyboardButton.builder()
                                            .text(new String(new byte[]{(byte) 0xE2, (byte) 0x9D, (byte) 0x8C}, StandardCharsets.UTF_8) + " " + ConfirmationFeedback.NO.name())
                                            .callbackData(mapper.writeValueAsString(getCallbackPayload(callbackPayload, ConfirmationFeedback.NO)))
                                            .build()))
                    ).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CallbackPayload getCallbackPayload(CallbackPayload callbackPayload, ConfirmationFeedback confirmationFeedback) {
        return CallbackPayload.builder()
                .callbackCommand(callbackPayload.getCallbackCommand())
                .date(callbackPayload.getDate())
                .sheetRowNumber(callbackPayload.getSheetRowNumber())
                .confirmationAnswer(confirmationFeedback.name()).build();
    }

    public InlineKeyboardMarkup getApproveDeclineButtonsMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(getApproveDeclineButtons());

        return inlineKeyboardMarkup;
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
                .date(date).sheetRowNumber(participation.getSheetRowNumber()).callbackCommand(CallbackCommand.TAKE_ROLE).build();
        try {
            return InlineKeyboardButton.builder()
                    .text(new String(new byte[]{(byte) 0xF0, (byte) 0x9F, (byte) 0x9A, (byte) 0xA9}, StandardCharsets.UTF_8)
                            + " " + participation.getEventRole()).callbackData(mapper.writeValueAsString(payload)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InlineKeyboardButton getDateButton(LocalDate date, CallbackCommand command) {
        CallbackPayload payload = CallbackPayload.builder().date(date).callbackCommand(command).build();
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
