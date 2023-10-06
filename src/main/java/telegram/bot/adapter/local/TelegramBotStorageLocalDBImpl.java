package telegram.bot.adapter.local;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import telegram.bot.adapter.TelegramBotStorage;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component("local")
public class TelegramBotStorageLocalDBImpl implements TelegramBotStorage {

    @Override
    public User saveUser(User user) {
        return null;
    }

    @Override
    public User getUserByTelegram(String telegram) {
        return null;
    }

    @Override
    public User getUserByCode(String code) {
        return null;
    }

    @Override
    public List<User> getUsers() {
        return null;
    }

    @Override
    public List<User> getPartisipansByDate(LocalDate date) {
        return null;
    }

    @Override
    public Participation saveParticipation(Participation participation) {
        return null;
    }

    @Override
    public void delteParticipation(Participation participation) {

    }
}
