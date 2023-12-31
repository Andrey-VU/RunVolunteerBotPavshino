package telegram.bot.adapter;

import telegram.bot.model.Participation;
import telegram.bot.model.Volunteer;

import java.time.LocalDate;
import java.util.List;

public interface TelegramBotStorage {

    /**
     * Записываем нового участника
     *
     * @param volunteer - Новый пользователь
     * @return Возвращает участника
     */
    Volunteer saveVolunteer(Volunteer volunteer);

    // обновляем данные для уже зарегистрированного участника
    default Volunteer updateVolunteer(Volunteer volunteer) {
        return null;
    }

    /**
     * Получаем участника по его телеграму
     *
     * @param tgUserName - Телеграм пользователя
     * @return Пользователь по телеграму
     */
    Volunteer getVolunteerByTgUserName(String tgUserName);

    /**
     * Получаем участника по его коду
     *
     * @param code - Код пользователя в системе 5 верст
     * @return Пользователь по телеграму
     */
    Volunteer getVolunteerByCode(String code);

    /**
     * Получаем всех участников
     *
     * @return Полный список всенх заргестрированных пользователей
     */
    List<Volunteer> getVolunteers();

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
