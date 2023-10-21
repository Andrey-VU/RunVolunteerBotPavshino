package telegram.bot.config;

import lombok.Getter;

@Getter
public enum BotModes {

    LOCAL("local"),

    GOOGLE("google");

    private final String code;

    BotModes(String code) {
        this.code = code;
    }

}
