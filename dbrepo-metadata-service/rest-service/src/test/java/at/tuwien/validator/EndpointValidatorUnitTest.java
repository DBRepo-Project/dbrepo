package at.tuwien.validator;

import at.tuwien.BaseUnitTest;
import at.tuwien.SortType;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.validation.EndpointValidator;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class EndpointValidatorUnitTest extends BaseUnitTest {

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private IdentifierRepository identifierRepository;

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
        DATABASE_1.setAccesses(List.of(DATABASE_1_USER_1_READ_ACCESS));
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
    public void validateOnlyAccessOrPublic_publicAnonymous_succeeds() throws DatabaseNotFoundException,
            NotAllowedException, AccessDeniedException {

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
            AccessDeniedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        doThrow(AccessDeniedException.class)
                .when(accessService)
                .find(DATABASE_1_ID, USER_1_ID);

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasReadAccess_succeeds() throws DatabaseNotFoundException,
            NotAllowedException, AccessDeniedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasWriteOwnAccess_succeeds() throws DatabaseNotFoundException,
            NotAllowedException, AccessDeniedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyAccessOrPublic_privateHasWriteAllAccess_succeeds() throws DatabaseNotFoundException,
            NotAllowedException, AccessDeniedException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyAccessOrPublic(DATABASE_1_ID, USER_1_PRINCIPAL);
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
            TableNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* mock */
        when(tableService.find(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasWriteOwnAccess_succeeds() throws NotAllowedException,
            TableNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* mock */
        when(tableService.find(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void validateOnlyWriteOwnOrWriteAllAccess_privateHasWriteAllAccess_succeeds() throws NotAllowedException,
            TableNotFoundException, DatabaseNotFoundException, AccessDeniedException {
        final Table table = Table.builder()
                .ownedBy(USER_2_ID)
                .build();

        /* mock */
        when(tableService.find(DATABASE_1_ID, 9999L))
                .thenReturn(table);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        endpointValidator.validateOnlyWriteOwnOrWriteAllAccess(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
    }

    @Test
    public void validateColumnCreateConstraints_empty_fails() {

        /* test */
        assertThrows(TableMalformedException.class, () -> {
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
        assertThrows(TableMalformedException.class, () -> {
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
        assertThrows(TableMalformedException.class, () -> {
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
        assertThrows(TableMalformedException.class, () -> {
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
        assertThrows(TableMalformedException.class, () -> {
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
        assertThrows(TableMalformedException.class, () -> {
            endpointValidator.validateColumnCreateConstraints(request);
        });
    }

    @Test
    public void validateColumnCreateConstraints_dateFormatEmpty_succeeds() throws TableMalformedException {
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
    public void validateOnlyOwnerOrWriteAll_noPrincipal_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyOwnerOrWriteAll(DATABASE_1_ID, TABLE_1_ID, null);
        });
    }

    @Test
    public void validateOnlyOwnerOrWriteAll_onlyReadAccess_fails() throws DatabaseNotFoundException,
            TableNotFoundException, AccessDeniedException {

        /* mock */
        when(tableService.find(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1);
        when(accessService.find(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyOwnerOrWriteAll(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void validateOnlyPrivateHasRole_privatePrincipalMissing_fails() throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateHasRole(DATABASE_1_ID, null, "list-tables");
        });
    }

    @Test
    public void validateOnlyPrivateHasRole_privateRoleMissing_fails() throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            endpointValidator.validateOnlyPrivateHasRole(DATABASE_1_ID, USER_4_PRINCIPAL, "list-tables");
        });
    }

}
