package telegram.bot.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import telegram.bot.service.enums.ButtonType;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true)
@FieldDefaults(level= AccessLevel.PRIVATE)
public class CallbackPayload {
    @JsonProperty("b")
    final ButtonType buttonType;

    @JsonProperty("d")
    @JsonFormat(pattern = "dd.MM.yyyy")
    final LocalDate date;

    @JsonProperty("c")
    String userChoice;

    @JsonProperty("r")
    int sheetRowNumber;
}
