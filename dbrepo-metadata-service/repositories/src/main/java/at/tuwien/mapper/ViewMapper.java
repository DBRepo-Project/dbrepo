package at.tuwien.mapper;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.ViewColumn;
import at.tuwien.entities.database.table.columns.TableColumn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ContainerMapper.class, UserMapper.class, TableMapper.class,
        IdentifierMapper.class})
public interface ViewMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewMapper.class);

    @Named("internalNameMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Mappings({
            @Mapping(target = "database.views", ignore = true),
            @Mapping(target = "database.tables", ignore = true),
            @Mapping(target = "database.identifiers", ignore = true),
    })
    ViewDto viewToViewDto(View data);

    ViewBriefDto viewToViewBriefDto(View data);

    @Transactional(readOnly = true)
    default TableColumn viewColumnToTableColumn(ViewColumn data) {
        return data.getColumn()
                .toBuilder()
                .alias(data.getAlias())
                .build();
    }

    default List<ViewColumn> tableColumnsToViewColumns(View view, List<TableColumn> data) {
        final int[] idx = new int[]{0};
        return data.stream()
                .map(c -> ViewColumn.builder()
                        .ordinalPosition(idx[0]++)
                        .column(c)
                        .view(view)
                        .alias(c.getAlias())
                        .build())
                .toList();
    }

}
