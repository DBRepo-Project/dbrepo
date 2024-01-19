package at.tuwien.service;

import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.exception.BannerMessageNotFoundException;

import java.util.List;

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
     * @throws BannerMessageNotFoundException The message was not found in the metadata database.
     */
    BannerMessage find(Long id) throws BannerMessageNotFoundException;

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
     * @param id   The message id.
     * @param data The updated message data.
     * @return The updated message, if successful.
     * @throws BannerMessageNotFoundException The message was not found in the metadata database.
     */
    BannerMessage update(Long id, BannerMessageUpdateDto data) throws BannerMessageNotFoundException;

    /**
     * Deletes a maintenance message by given id in the metadata database.
     *
     * @param id The message id.
     * @throws BannerMessageNotFoundException The message was not found in the metadata database.
     */
    void delete(Long id) throws BannerMessageNotFoundException;
}
