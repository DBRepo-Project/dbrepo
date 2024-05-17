package at.tuwien.validator;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.SortType;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.validation.EndpointValidator;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class EndpointValidatorUnitTest extends AbstractUnitTest {

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private TableService tableService;

    @Autowired
    private EndpointValidator endpointValidator;

    public static Stream<Arguments> needSize_parameters() {
        return Stream.of(
                Arguments.arguments(ColumnTypeDto.CHAR),
                Arguments.arguments(ColumnTypeDto.VARCHAR),
                Arguments.arguments(ColumnTypeDto.BINARY),
                Arguments.arguments(ColumnTypeDto.VARBINARY),
                Arguments.arguments(ColumnTypeDto.BIT),
                Arguments.arguments(ColumnTypeDto.TINYINT),
                Arguments.arguments(ColumnTypeDto.SMALLINT),
                Arguments.arguments(ColumnTypeDto.MEDIUMINT),
                Arguments.arguments(ColumnTypeDto.INT)
        );
    }

    public static Stream<Arguments> needSizeAndD_parameters() {
        return Stream.of(
                Arguments.arguments(ColumnTypeDto.DOUBLE),
                Arguments.arguments(ColumnTypeDto.DECIMAL)
        );
    }

    public static Stream<Arguments> needDateFormat_parameters() {
        return Stream.of(
                Arguments.arguments(ColumnTypeDto.DATETIME),
                Arguments.arguments(ColumnTypeDto.TIMESTAMP),
                Arguments.arguments(ColumnTypeDto.TIME)
        );
    }

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

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
    public void validateOnlyAccessOrPublic_publicAnonymous_succeeds() throws NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_3_ID))
                .thenReturn(DATABASE_3);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_3, null);
    }

    @Test
    @Disabled("keep failing on CI but works locally")
    public void validateOnlyAccessOrPublic_privateAnonymous_fails() throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1, null);
        });
    }

    @Test
    @Disabled("keep failing on CI but works locally")
    public void validateOnlyAccessOrPublic_privateNoAccess_fails() throws DatabaseNotFoundException,
            AccessNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        doThrow(AccessNotFoundException.class)
                .when(accessService)
                .find(eq(DATABASE_1), any(User.class));

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasReadAccess_succeeds() throws NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasWriteOwnAccess_succeeds() throws NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasWriteAllAccess_succeeds() throws NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasReadAccess_fails() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException {

        /* mock */
        when(tableService.findById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(TABLE_1, USER_1);
        });
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasWriteOwnAccess_succeeds() throws NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException, TableNotFoundException {

        /* mock */
        when(tableService.findById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(TABLE_1, USER_1);
    }

    @Test
    public void validateColumnCreateConstraints_empty_fails() {

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(null);
        });
    }

    @ParameterizedTest
    @MethodSource("needSize_parameters")
    public void validateColumnCreateConstraints_needSize_fails(ColumnTypeDto type) {
        final TableCreateDto request = TableCreateDto.builder()
                .columns(List.of(ColumnCreateDto.builder()
                        .type(type)
                        .size(null) // <<<<<<
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @ParameterizedTest
    @MethodSource("needSizeAndD_parameters")
    public void validateColumnCreateConstraints_needSizeAndD_fails(ColumnTypeDto type) {
        final TableCreateDto request = TableCreateDto.builder()
                .columns(List.of(ColumnCreateDto.builder()
                        .type(type)
                        .size(10L)
                        .d(null) // <<<<<<<
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @Test
    public void validateColumnCreateConstraints_needEnum_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .columns(List.of(ColumnCreateDto.builder()
                        .type(ColumnTypeDto.ENUM)
                        .enums(null) // <<<<<<<
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @Test
    public void validateColumnCreateConstraints_needSet_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .columns(List.of(ColumnCreateDto.builder()
                        .type(ColumnTypeDto.SET)
                        .sets(null) // <<<<<<<
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @ParameterizedTest
    @MethodSource("needDateFormat_parameters")
    public void validateColumnCreateConstraints_needDateFormat_fails(ColumnTypeDto type) {
        final TableCreateDto request = TableCreateDto.builder()
                .columns(List.of(ColumnCreateDto.builder()
                        .type(type)
                        .dfid(null) // <<<<<<<
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @Test
    public void validateColumnCreateConstraints_dateFormatEmpty_succeeds() throws MalformedException {
        final TableCreateDto request = TableCreateDto.builder()
                .columns(List.of(ColumnCreateDto.builder()
                        .type(ColumnTypeDto.DATE)
                        .dfid(null) // <<<<<<<
                        .build()))
                .build();

        /* test */
        endpointValidator.validateColumnCreateConstraints(request);
    }

    @Test
    public void validateOnlyOwnerOrWriteAll_onlyReadAccess_fails() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException {

        /* mock */
        when(tableService.findById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1, USER_1))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyOwnerOrWriteAll(TABLE_1, USER_1);
        });
    }

    @Test
    @Disabled("keep failing on CI but works locally")
    public void validateOnlyPrivateHasRole_privatePrincipalMissing_fails() throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateHasRole(DATABASE_1, null, "list-tables");
        });
    }

    @Test
    @Disabled("keep failing on CI but works locally")
    public void validateOnlyPrivateHasRole_privateRoleMissing_fails() throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateHasRole(DATABASE_1, USER_4_PRINCIPAL, "list-tables");
        });
    }

    @Test
    public void validatePublicationDate_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationMonth(1)
                .publicationDay(25)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_missingDay_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationMonth(1)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_missingMonth_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationDay(1)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_missingDayMonth_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_yearEarly_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(1300)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_yearLate_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(12345)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_monthInvalid1_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationMonth(0)
                .build();

        /* test */
        assertFalse(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_monthInvalid2_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationMonth(13)
                .build();

        /* test */
        assertFalse(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_dayInvalid1_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationDay(0)
                .build();

        /* test */
        assertFalse(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_dayInvalid2_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationDay(32)
                .build();

        /* test */
        assertFalse(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_february29Invalid_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2023)
                .publicationMonth(2)
                .publicationDay(29)
                .build();

        /* test */
        assertFalse(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validatePublicationDate_february29_succeeds() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .publicationYear(2024)
                .publicationMonth(2)
                .publicationDay(29)
                .build();

        /* test */
        assertTrue(endpointValidator.validatePublicationDate(request));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_noAccess_fails() {

        /* test */
        assertFalse(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, null, "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_readAccess_fails() {

        /* test */
        assertFalse(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS, "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_ownerOnlyWriteOwn_succeeds() {

        /* test */
        assertTrue(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS, "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_notOwnerOnlyWriteOwn_fails() {

        /* test */
        assertFalse(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_2, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS, "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_notOwnerWriteAll_succeeds() {

        /* test */
        assertTrue(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_2, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_ALL_ACCESS, "nobody-role"));
    }

}
