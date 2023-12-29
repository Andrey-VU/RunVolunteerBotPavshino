package telegram.bot.model;

import lombok.Data;
import telegram.bot.service.enums.RegistrationStages;

@Data
public class RegistrationForm {

    private RegistrationStages stage = RegistrationStages.NEW;

    private String name;

    private String surname;

    private String code;
}
