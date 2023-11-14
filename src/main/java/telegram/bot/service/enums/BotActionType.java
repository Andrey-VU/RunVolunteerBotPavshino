package telegram.bot.service.enums;

import lombok.Getter;

@Getter
public enum BotActionType {
    UNDEFINED(null),
    SAVE("Записаться в волонтеры"),
    SHOW("Показать, кто уже записан");

    private final String menuCaption = "Выберите действие";
    private final String buttonCaption;

    BotActionType(String buttonCaption) {
        this.buttonCaption = buttonCaption;
    }
}