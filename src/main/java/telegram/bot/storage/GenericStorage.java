package telegram.bot.storage;

import telegram.bot.model.Event;
import telegram.bot.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public abstract class GenericStorage {
    protected List<User> users;
    protected Map<LocalDate, Event> events;
}
