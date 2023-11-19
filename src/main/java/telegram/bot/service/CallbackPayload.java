package telegram.bot.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("c")
    private final Callbackcommands command;

    @JsonProperty("d")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private final LocalDate date;

    @JsonIgnore
    private String role;

    @JsonProperty("r")
    private int sheetRowNumber;
}
