package telegram.bot.service;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Getter
@Component
public class Elements {
    private final InlineKeyboardButton saveChooser;
    private final InlineKeyboardButton showChooser;
    private Collection<InlineKeyboardButton> saturdays;
    private ReplyKeyboard menu;

    public Elements() {
        saveChooser = InlineKeyboardButton.builder()
                .text("Записаться в волонтеры")
                .callbackData("save")
                .build();

        showChooser = InlineKeyboardButton.builder()
                .text("Показать, кто уже записан")
                .callbackData("show")
                .build();

        saturdays = new LinkedList<>();
    }

    public Collection<InlineKeyboardButton> getSaturdays() {
        saturdays.clear();
        return saturdays;
    }

    public ReplyKeyboard getMainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(saveChooser, showChooser))
                .build();
    }

}