package telegram.bot.storage;

import telegram.bot.model.Event;
import telegram.bot.model.User;

import java.time.LocalDate;
import java.util.Map;

public abstract class Storage {
    protected Map<String, User> contacts;
    protected Map<LocalDate, Event> events;
}
