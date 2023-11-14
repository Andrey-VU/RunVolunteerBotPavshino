package telegram.bot.service.enums;

import lombok.Getter;

@Getter
public enum BotActionType {
    UNDEFINED(null),
    SAVE("Записаться в волонтеры"),
    SHOW("Показать, кто уже записан");

    @Getter
    private static final String menuCaption = "Выберите действие";
    private final String buttonCaption;

    public static BotActionType getBotActionType(String botActionTypeValue) {
        return BotActionType.valueOf(botActionTypeValue);
    }

    BotActionType(String buttonCaption) {
        this.buttonCaption = buttonCaption;
    }
}