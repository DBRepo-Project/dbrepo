package at.ac.tuwien.ifs.dbrepo.validator;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.SortType;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierSaveDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import lombok.extern.log4j.Log4j2;
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
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class EndpointValidatorUnitTest extends BaseTest {

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
                Arguments.arguments("varchar", ColumnTypeDto.VARCHAR),
                Arguments.arguments("binary", ColumnTypeDto.BINARY),
                Arguments.arguments("varbinary", ColumnTypeDto.VARBINARY)
        );
    }

    public static Stream<Arguments> needSizeAndD_parameters() {
        return Stream.of(
                Arguments.arguments("double_size", ColumnTypeDto.DOUBLE, 40L, null),
                Arguments.arguments("double_d", ColumnTypeDto.DOUBLE, null, 10L),
                Arguments.arguments("decimal_size", ColumnTypeDto.DECIMAL, 40L, null),
                Arguments.arguments("decimal_d", ColumnTypeDto.DECIMAL, null, 10L)
        );
    }

    public static Stream<Arguments> enums_parameters() {
        return Stream.of(
                Arguments.arguments("enums_null", ColumnTypeDto.ENUM, null),
                Arguments.arguments("enums_empty", ColumnTypeDto.ENUM, List.of()),
                Arguments.arguments("sets_null", ColumnTypeDto.SET, null),
                Arguments.arguments("sets_empty", ColumnTypeDto.SET, List.of())
        );
    }

    @Test
    public void validateDataParams_succeeds() throws PaginationException {

        /* test */
        endpointValidator.validateDataParams(0L, 1L);
    }

    @Test
    public void validateOnlyAccess_system_succeeds() throws UserNotFoundException, NotAllowedException,
            AccessNotFoundException {

        /* test */
        endpointValidator.validateOnlyAccess(DATABASE_1, USER_LOCAL_ADMIN_PRINCIPAL, false);
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
        when(databaseService.findById(any(UUID.class)))
                .thenReturn(DATABASE_3);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_3, null);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1, null);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic_privateNoAccess_fails() throws AccessNotFoundException {

        /* mock */
        doThrow(AccessNotFoundException.class)
                .when(accessService)
                .find(any(Database.class), any(User.class));

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
                .thenReturn(DATABASE_1.getAccesses().get(0));

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
    public void validateOnlyWriteOwnOrWriteAllAccess_succeeds() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException, NotAllowedException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(TABLE_1, USER_1);
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_writeOwnAccess_succeeds() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException, NotAllowedException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(TABLE_1, USER_1);
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasReadAccess_fails() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(eq(DATABASE_1), any(User.class)))
                .thenReturn(DATABASE_1.getAccesses().get(0));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(TABLE_1, USER_1);
        });
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasWriteOwnAccess_succeeds() throws NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException, TableNotFoundException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
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
    public void validateColumnCreateConstraints_needSize_fails(String name, ColumnTypeDto type) {
        final CreateTableDto request = CreateTableDto.builder()
                .columns(List.of(CreateTableColumnDto.builder()
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
    @MethodSource("enums_parameters")
    public void validateColumnCreateConstraints_needEnum_fails(String name, ColumnTypeDto type, List<String> enums) {
        final CreateTableDto request = CreateTableDto.builder()
                .columns(List.of(CreateTableColumnDto.builder()
                        .type(type)
                        .enums(enums)
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @ParameterizedTest
    @MethodSource("needSizeAndD_parameters")
    public void validateColumnCreateConstraints_needSizeAndD_fails(String name, ColumnTypeDto type, Long size, Long d) {
        final CreateTableDto request = CreateTableDto.builder()
                .columns(List.of(CreateTableColumnDto.builder()
                        .type(type)
                        .size(size)
                        .d(d)
                        .build()))
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @Test
    public void validateOnlyOwnerOrWriteAll_onlyReadAccess_fails() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1, USER_1))
                .thenReturn(DATABASE_1.getAccesses().get(0));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyOwnerOrWriteAll(TABLE_1, USER_1);
        });
    }

    @Test
    public void validateOnlyOwnerOrWriteAll_writeOwnAccess_succeeds() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException, NotAllowedException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1, USER_1))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyOwnerOrWriteAll(TABLE_1, USER_1);
    }

    @Test
    public void validateOnlyOwnerOrWriteAll_writeAllAccess_succeeds() throws DatabaseNotFoundException,
            TableNotFoundException, AccessNotFoundException, NotAllowedException {

        /* mock */
        when(tableService.findById(DATABASE_1, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1, USER_2))
                .thenReturn(DATABASE_1_USER_2_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyOwnerOrWriteAll(TABLE_1, USER_2);
    }

    @Test
    public void validateOnlyPrivateDataHasRole_publicDatabase_succeeds() throws NotAllowedException {

        /* test */
        endpointValidator.validateOnlyPrivateDataHasRole(DATABASE_4, null, "nobody-role");
    }

    @Test
    public void validateOnlyPrivateDataHasRole_privateDatabaseHasRole_succeeds() throws NotAllowedException {

        /* test */
        endpointValidator.validateOnlyPrivateDataHasRole(DATABASE_1, USER_1_PRINCIPAL, "find-database");
    }

    @Test
    public void validateOnlyPrivateDataHasRole_privatePrincipalMissing_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateDataHasRole(DATABASE_1, null, "list-tables");
        });
    }

    @Test
    public void validateOnlyPrivateDataHasRole_privateRoleMissing_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateDataHasRole(DATABASE_1, USER_4_PRINCIPAL, "list-tables");
        });
    }

    @Test
    public void validateOnlyPrivateSchemaHasRole_privatePrincipalMissing_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateSchemaHasRole(DATABASE_1, null, "list-tables");
        });
    }

    @Test
    public void validateOnlyPrivateSchemaHasRole_privateRoleMissing_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateSchemaHasRole(DATABASE_1, USER_4_PRINCIPAL, "list-tables");
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
    public void validateOnlyMineOrWriteAccessOrHasRole_succeeds() {

        /* test */
        assertTrue(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, null, "find-database"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_noAccess_fails() {

        /* test */
        assertFalse(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, null, "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_readAccess_fails() {

        /* test */
        assertFalse(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, DATABASE_1.getAccesses().get(0), "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_ownerOnlyWriteOwn_succeeds() {

        /* test */
        assertTrue(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS, "nobody-role"));
    }

    @Test
    public void validateOnlyMineOrWriteAccessOrHasRole_ownerOnlyWriteAll_succeeds() {

        /* test */
        assertTrue(endpointValidator.validateOnlyMineOrWriteAccessOrHasRole(USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_ALL_ACCESS, "nobody-role"));
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
