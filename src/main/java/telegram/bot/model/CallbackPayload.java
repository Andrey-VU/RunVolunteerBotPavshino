package telegram.bot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import telegram.bot.service.enums.ButtonType;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class CallbackPayload {
    @JsonProperty("b")
    private final ButtonType buttonType;

    @JsonProperty("d")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private final LocalDate date;

    @JsonProperty("r")
    private int sheetRowNumber;

    @JsonProperty("c")
    private String userChoice;
}
