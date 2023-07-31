class QueryMapper {
  mySql8DataTypes () {
    return [
      { value: 'bool', text: 'BOOL' },
      { value: 'varchar', text: 'VARCHAR(size)', defaultSize: 255 },
      { value: 'text', text: 'TEXT' },
      { value: 'int', text: 'INT(size)', defaultSize: 255 },
      { value: 'char', text: 'CHAR(size)', defaultSize: 1 },
      { value: 'binary', text: 'BINARY(size)', defaultSize: 1 },
      { value: 'varbinary', text: 'VARBINARY(size)', defaultSize: 1 },
      { value: 'tinyblob', text: 'TINYBLOB' },
      { value: 'tinytext', text: 'TINYTEXT' },
      { value: 'blob', text: 'BLOB' },
      { value: 'mediumtext', text: 'MEDIUMTEXT' },
      { value: 'mediumblob', text: 'MEDIUMBLOB' },
      { value: 'longtext', text: 'LONGTEXT' },
      { value: 'longblob', text: 'LONGBLOB' },
      { value: 'enum', text: 'ENUM(val1,val2,...)' },
      { value: 'set', text: 'SET(val1,val2,...)' },
      { value: 'bit', text: 'BIT(size)', defaultSize: 1 },
      { value: 'tinyint', text: 'TINYINT(size)', defaultSize: 10 },
      { value: 'smallint', text: 'SMALLINT(size)', defaultSize: 10 },
      { value: 'mediumint', text: 'MEDIUMINT(size)', defaultSize: 10 },
      { value: 'bigint', text: 'BIGINT(size)', defaultSize: 255 },
      { value: 'float', text: 'FLOAT(p)', defaultSize: 24 },
      { value: 'double', text: 'DOUBLE(size, d)', defaultSize: 25, defaultD: 0 },
      { value: 'decimal', text: 'DECIMAL(size, d)', defaultSize: 10, defaultD: 0 },
      { value: 'date', text: 'DATE' },
      { value: 'datetime', text: 'DATETIME(fsp)' },
      { value: 'timestamp', text: 'TIMESTAMP(fsp)' },
      { value: 'time', text: 'TIME(fsp)' },
      { value: 'year', text: 'YEAR' }
    ]
  }
}

export default new QueryMapper()
