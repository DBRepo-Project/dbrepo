class QueryMapper {
  mySql8DataTypes () {
    return [
      { value: 'bigint', text: 'BIGINT(size)', defaultSize: 255 },
      { value: 'binary', text: 'BINARY(size)', defaultSize: 1 },
      { value: 'bit', text: 'BIT(size)', defaultSize: 1 },
      { value: 'blob', text: 'BLOB' },
      { value: 'bool', text: 'BOOL' },
      { value: 'char', text: 'CHAR(size)', defaultSize: 1 },
      { value: 'date', text: 'DATE' },
      { value: 'datetime', text: 'DATETIME(fsp)' },
      { value: 'decimal', text: 'DECIMAL(size, d)', defaultSize: 10, defaultD: 4 },
      { value: 'double', text: 'DOUBLE(size, d)', defaultSize: 25, defaultD: 4 },
      { value: 'enum', text: 'ENUM(val1,val2,...)' },
      { value: 'float', text: 'FLOAT(p)', defaultSize: 24 },
      { value: 'int', text: 'INT(size)', defaultSize: 255 },
      { value: 'longblob', text: 'LONGBLOB' },
      { value: 'longtext', text: 'LONGTEXT' },
      { value: 'mediumblob', text: 'MEDIUMBLOB' },
      { value: 'mediumint', text: 'MEDIUMINT(size)', defaultSize: 10 },
      { value: 'mediumtext', text: 'MEDIUMTEXT' },
      { value: 'set', text: 'SET(val1,val2,...)' },
      { value: 'smallint', text: 'SMALLINT(size)', defaultSize: 10 },
      { value: 'text', text: 'TEXT' },
      { value: 'time', text: 'TIME(fsp)' },
      { value: 'timestamp', text: 'TIMESTAMP(fsp)' },
      { value: 'tinyblob', text: 'TINYBLOB' },
      { value: 'tinyint', text: 'TINYINT(size)', defaultSize: 10 },
      { value: 'tinytext', text: 'TINYTEXT' },
      { value: 'year', text: 'YEAR' },
      { value: 'varbinary', text: 'VARBINARY(size)', defaultSize: 1 },
      { value: 'varchar', text: 'VARCHAR(size)', defaultSize: 255 }
    ]
  }
}

export default new QueryMapper()
