package telegram.bot.config;

public enum BotModes {

    LOCAL("TelegramBotStorageLocalDBImpl"),

    GOOGLE("TelegramBotStorageGoogleTableImpl");

    private final String code;

    BotModes(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
