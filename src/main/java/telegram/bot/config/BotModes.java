package telegram.bot.config;

public enum BotModes {

    LOCAL("local"),

    GOOGLE("google");

    private final String code;

    BotModes(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
