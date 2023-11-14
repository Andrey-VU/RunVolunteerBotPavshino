package telegram.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import telegram.bot.config.BotConfiguration;
import telegram.bot.service.enums.BotActionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

@Getter
@Component
public class BotElement {
    private List<InlineKeyboardButton> saturdays;

    private InlineKeyboardButton saveChooser;
    private InlineKeyboardButton showChooser;

    @PostConstruct
    private void init() {
        saturdays = new LinkedList<>();
        saveChooser = InlineKeyboardButton.builder()
                .text(BotActionType.SAVE.getButtonCaption())
                .callbackData(BotActionType.SAVE.name())
                .build();

        showChooser = InlineKeyboardButton.builder()
                .text(BotActionType.SHOW.getButtonCaption())
                .callbackData(BotActionType.SAVE.name())
                .build();

    }

    public ReplyKeyboard getMainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(saveChooser, showChooser))
                .build();
    }

    public ReplyKeyboard getSaturdaysMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(getSaturdays())
                .build();
    }

    public List<InlineKeyboardButton> getSaturdays() {
        saturdays.clear();

        var datePatternString = "dd.MM.yyyy";
        var nextSaturdaysCounter = 0;
        LocalDate saturday = LocalDate.now();

        while (nextSaturdaysCounter < BotConfiguration.getSheetSaturdaysAhead()) {
            saturday = getNextSaturday(saturday);
            saturdays.add(InlineKeyboardButton.builder()
                    .text(saturday.format(DateTimeFormatter.ofPattern(datePatternString)))
                    .callbackData(saturday.format(DateTimeFormatter.ofPattern(datePatternString)))
                    .build());
            nextSaturdaysCounter++;
        }

        return saturdays;
    }

    private LocalDate getNextSaturday(LocalDate day) {
        do day = day.plusDays(1);
        while (day.getDayOfWeek() != DayOfWeek.SATURDAY);
        return day;
    }
}