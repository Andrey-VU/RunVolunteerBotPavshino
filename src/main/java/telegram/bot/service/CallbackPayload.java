package telegram.bot.service;

import lombok.Builder;
import lombok.Data;
import telegram.bot.service.enums.Callbackcommands;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CallbackPayload {

    private final Callbackcommands command;

    private final LocalDate date;

    private String role;
}
