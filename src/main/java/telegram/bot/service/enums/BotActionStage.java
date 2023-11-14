package telegram.bot.service.enums;

public enum BotActionStage {
    UNDEFINED,
    PROMPT_FOR_ACTION_TYPE,
    PROMPT_FOR_SATURDAY,
    PROMPT_FOR_NAME,
    PROMPT_FOR_SURNAME,
    PROMPT_FOR_CODE,
    PROMPT_FOR_ROLE,
    ROLE_CONFIRMATION,
    LIST_BUSY_ROLES
}
