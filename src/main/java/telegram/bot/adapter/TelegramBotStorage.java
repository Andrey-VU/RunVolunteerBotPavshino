package telegram.bot.adapter;

import org.springframework.stereotype.Component;
import telegram.bot.model.Participation;
import telegram.bot.model.User;

import java.time.LocalDate;
import java.util.List;

@Component
public interface TelegramBotStorage {

    /**
     * Записываем нового участника
     *
     * @param user - Новый пользователь
     * @return Возвращает участника
     */
    User saveUser(User user);

    // обновляем данные для уже зарегистрированного участника
    default User updateUser(User user) {
        return null;
    }

    /**
     * Получаем участника по его телеграму
     *
     * @param telegram - Телеграм пользователя
     * @return Пользователь по телеграму
     */
    User getUserByTelegram(String telegram);

    /**
     * Получаем участника по его коду
     *
     * @param code - Код пользователя в системе 5 верст
     * @return Пользователь по телеграму
     */
    User getUserByCode(String code);

    /**
     * Получаем всех участников
     *
     * @return Полный список всенх заргестрированных пользователей
     */
    List<User> getUsers();

    /**
     * Получаем список участников на дату
     *
     * @param date - Дата за котроую хотим получить список участников
     * @return Список участников
     */
    List<Participation> getParticipantsByDate(LocalDate date);

    // получаем список организаторов
    List<String> getOrganizers();

    /**
     * Получаем список свободных позиций на дату
     *
     * @param date - Дата, за которую хотим получить список участников
     * @return - Список свободных позиций на указанную дату
     */
    List<Participation> getAvailableParticipationByDate(LocalDate date);

    /**
     * Записываем участие
     *
     * @param participation - участие
     * @return Возвращаем участие записанное
     * Если вернуди null - запсь не уадалсь
     */
    Participation saveParticipation(Participation participation);

    /**
     * Отменяем участие
     *
     * @param participation - Участие которое удаляем
     */
    void deleteParticipation(Participation participation);

}
