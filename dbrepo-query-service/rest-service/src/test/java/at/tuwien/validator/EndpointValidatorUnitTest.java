package at.tuwien.validator;

import at.tuwien.BaseUnitTest;
import at.tuwien.SortType;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.VisibilityType;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.repository.sdb.ViewIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.validation.EndpointValidator;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class EndpointValidatorUnitTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    @MockBean
    private TableService tableService;

    @Autowired
    private EndpointValidator endpointValidator;

    @Test
    public void validateDataParams_succeeds() throws PaginationException {

        /* test */
        endpointValidator.validateDataParams(0L, 1L);
    }

    @Test
    public void validateDataParams_bothNull_succeeds() throws PaginationException {

        /* test */
        endpointValidator.validateDataParams(null, null);
    }

    @Test
    public void validateDataParams_pageNull_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(null, 1L);
        });
    }

    @Test
    public void validateDataParams_sizeNull_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(0L, null);
        });
    }

    @Test
    public void validateDataParams_pageTooLow_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(-1L, 1L);
        });
    }

    @Test
    public void validateDataParams_sizeTooLow_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(0L, 0L);
        });
    }

    @Test
    public void validateDataParams2_bothNull_succeeds() throws SortException, PaginationException {

        /* test */
        endpointValidator.validateDataParams(0L, 1L, null, null);
    }

    @Test
    public void validateDataParams2_succeeds() throws SortException, PaginationException {

        /* test */
        endpointValidator.validateDataParams(0L, 1L, SortType.ASC, "id");
    }

    @Test
    public void validateDataParams2_sortTypeNull_fails() {

        /* test */
        assertThrows(SortException.class, () -> {
            endpointValidator.validateDataParams(0L, 1L, null, "id");
        });
    }

    @Test
    public void validateDataParams2_sortColumnNull_fails() {

        /* test */
        assertThrows(SortException.class, () -> {
            endpointValidator.validateDataParams(0L, 1L, SortType.ASC, null);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic_publicAnonymous_succeeds() throws DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(DATABASE_3_ID))
                .thenReturn(DATABASE_3);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_3_ID, null);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateAnonymous_fails() throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, null);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic_privateNoAccess_fails() throws DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        doThrow(NotAllowedException.class)
                .when(accessService)
                .find(DATABASE_1_ID, USER_1_USERNAME);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasReadAccess_succeeds() throws DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasWriteOwnAccess_succeeds() throws DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasWriteAllAccess_succeeds() throws DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic2_privateAnonymousHasPublicIdentifier_succeeds() throws DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, QUERY_1_ID, null);
    }

    @Test
    public void validateOnlyAccessOrPublic2_privateAnonymousHasSelfIdentifier_fails() throws DatabaseNotFoundException {
        final Identifier identifier = Identifier.builder()
                .visibility(VisibilityType.SELF)
                .creator(USER_1)
                .build();

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(Optional.of(identifier));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, QUERY_1_ID, null);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic2_privateCreatorHasSelfIdentifier_succeeds() throws DatabaseNotFoundException,
            NotAllowedException {
        final Identifier identifier = Identifier.builder()
                .visibility(VisibilityType.SELF)
                .creator(USER_1)
                .build();

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(Optional.of(identifier));

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, TABLE_1_ID, null);
        });
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasReadAccess_fails() throws NotAllowedException,
            TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(tableService.find(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasWriteOwnAccess_succeeds() throws NotAllowedException,
            TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(tableService.find(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasWriteAllAccess_succeeds() throws NotAllowedException,
            TableNotFoundException, DatabaseNotFoundException {
        final Table table = Table.builder()
                .owner(USER_2)
                .build();

        /* mock */
        when(tableService.find(DATABASE_1_ID, 9999L))
                .thenReturn(table);
        when(accessService.find(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
    }

}
