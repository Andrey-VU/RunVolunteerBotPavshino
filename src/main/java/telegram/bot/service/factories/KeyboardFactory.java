package telegram.bot.service.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.bot.service.CallbackPayload;
import telegram.bot.service.DatesCalculator;
import telegram.bot.service.enums.Callbackcommands;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KeyboardFactory {

    ObjectMapper mapper = new ObjectMapper();

    public InlineKeyboardMarkup getFourDatesButton(Callbackcommands command) {
        List<LocalDate> dates = DatesCalculator.getNextEventDates();
        return InlineKeyboardMarkup.builder().keyboard(List.of(
                List.of(getDateButton(dates.get(0), command), getDateButton(dates.get(1), command)),
                List.of(getDateButton(dates.get(2), command), getDateButton(dates.get(3), command))
        )).build();
    }

    private InlineKeyboardButton getDateButton(LocalDate date, Callbackcommands command) {
        CallbackPayload payload = CallbackPayload.builder().date(date).command(command).build();
        try {
            return InlineKeyboardButton.builder()
                    .text(date.format(DateTimeFormatter.ISO_DATE))
                    .callbackData(mapper.writeValueAsString(payload)).build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
