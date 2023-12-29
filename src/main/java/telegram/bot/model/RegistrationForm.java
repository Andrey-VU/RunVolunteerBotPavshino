package telegram.bot.model;

import lombok.Data;
import telegram.bot.service.enums.CustomerJourneyStage;

@Data
public class RegistrationForm {
    private CustomerJourneyStage stage = CustomerJourneyStage.BEGIN;
    private String name;
    private String surname;
    private String code;
}
