class TableMapper {
  tableCreateToTableCreateDto (tableCreate) {
    return tableCreate.columns.reduce((table, column) => {
      // eslint-disable-next-line camelcase
      const { name, type, null_allowed, primary_key, size, d, enums, sets, dfid } = column
      table.columns.push({
        name,
        type,
        null_allowed,
        primary_key,
        dfid,
        size: size !== null && size !== false ? size : null,
        d: d !== null && d !== false ? d : null,
        enums: enums !== null ? enums : [],
        sets: sets !== null ? sets : []
      })
      if (column.unique) {
        table.constraints.uniques.push([column.name])
      }
      if (column.check_expression) {
        table.checks.push(column.check_expression)
      }
      if (column.foreign_key && column.references) {
        table.foreign_keys.push({
          columns: [column.name],
          referenced_table: column.foreign_key,
          referenced_columns: [column.references]
        })
      }
      return table
    }, {
      name: tableCreate.name,
      description: tableCreate.description,
      columns: [],
      constraints: {
        foreign_keys: [],
        uniques: [],
        checks: []
      }
    })
  }

  tableNameToInternalName (name) {
    return name.toString()
      .normalize('NFKD')
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '-')
      .replace(/[^\w-]+/g, '')
      .replace(/--+/g, '_')
  }
}

export default new TableMapper()
