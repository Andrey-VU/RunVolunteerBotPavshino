package telegram.bot.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;
import telegram.bot.service.enums.Callbackcommands;
import telegram.bot.service.helpers.CustomLocalDateDeserializer;
import telegram.bot.service.helpers.CustomLocalDateSerializer;
import telegram.bot.service.helpers.JsonCallbackCOmmandsDeserializer;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CallbackPayload {

    @JsonDeserialize(using = JsonCallbackCOmmandsDeserializer.class)
    private final Callbackcommands command;

    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @JsonDeserialize(using = CustomLocalDateDeserializer.class)
    private final LocalDate date;

    private String role;
}
