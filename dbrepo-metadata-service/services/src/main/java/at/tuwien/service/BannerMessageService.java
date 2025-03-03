package at.tuwien.service;

import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.exception.MessageNotFoundException;

import java.util.List;
import java.util.UUID;

public interface BannerMessageService {

    /**
     * Finds all messages in the metadata database.
     *
     * @return List of messages.
     */
    List<BannerMessage> findAll();

    /**
     * Finds all messages that are valid at the current point in time.
     *
     * @return List of active messages.
     */
    List<BannerMessage> getActive();

    /**
     * Finds a specific message by given id in the metadata database.
     *
     * @param id The message id.
     * @return The message, if successful.
     * @throws MessageNotFoundException The message was not found in the metadata database.
     */
    BannerMessage find(UUID id) throws MessageNotFoundException;

    /**
     * Creates a new maintenance message in the metadata database.
     *
     * @param data The message data.
     * @return The created message, if successful.
     */
    BannerMessage create(BannerMessageCreateDto data);

    /**
     * Updates a maintenance message by given id in the metadata database.
     *
     * @param message The message.
     * @param data    The updated message data.
     * @return The updated message, if successful.
     */
    BannerMessage update(BannerMessage message, BannerMessageUpdateDto data);

    /**
     * Deletes a maintenance message by given id in the metadata database.
     *
     * @param message The message.
     */
    void delete(BannerMessage message);
}
