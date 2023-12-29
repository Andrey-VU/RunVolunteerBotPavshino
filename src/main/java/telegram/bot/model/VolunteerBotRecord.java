package telegram.bot.model;

import lombok.Data;
import telegram.bot.service.enums.TgUserJourneyStage;

@Data
public class VolunteerBotRecord {
    private TgUserJourneyStage stage = TgUserJourneyStage.BEGIN;
    private String name;
    private String surname;
    private String code;
}
