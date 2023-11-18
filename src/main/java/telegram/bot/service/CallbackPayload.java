package telegram.bot.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import telegram.bot.service.enums.Callbackcommands;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class CallbackPayload {
    private final Callbackcommands command;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private final LocalDate date;

    private String role;
}
