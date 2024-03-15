import {format} from 'sql-formatter'
import type {AxiosRequestConfig} from 'axios'

export const useQueryService = (): any => {
  async function findAll(databaseId: number, persisted: boolean): Promise<QueryDto[]> {
    const axios = useAxiosInstance()
    console.debug('find queries')
    return new Promise<QueryDto[]>((resolve, reject) => {
      axios.get<QueryDto[]>(`/api/database/${databaseId}/query`, {params: (persisted && { persisted })})
        .then((response) => {
          console.info('Found query(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find queries', error)
          reject(error)
        })
    })
  }

  async function findOne(databaseId: number, queryId: number): Promise<QueryDto> {
    const axios = useAxiosInstance()
    console.debug('find query with id', queryId, 'in database with id', databaseId)
    return new Promise<QueryDto>((resolve, reject) => {
      axios.get<QueryDto>(`/api/database/${databaseId}/query/${queryId}`)
        .then((response) => {
          console.info('Found query with id', queryId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find query', error)
          reject(error)
        })
    })
  }

  async function update(databaseId: number, queryId: number, data: QueryPersistDto): Promise<QueryDto> {
    const axios = useAxiosInstance()
    console.debug('update query with id', queryId, 'in database with id', databaseId)
    return new Promise<QueryDto>((resolve, reject) => {
      axios.put<QueryDto>(`/api/database/${databaseId}/query/${queryId}`, data)
        .then((response) => {
          console.info('Updated query with id', queryId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update query', error)
          reject(error)
        })
    })
  }

  async function exportCsv(databaseId: number, queryId: number): Promise<any> {
    const axios = useAxiosInstance()
    const config: AxiosRequestConfig = {
      responseType: 'blob',
      headers: {
        Accept: 'text/csv'
      }
    }
    console.debug('export query with id', queryId, 'in database with id', databaseId)
    return new Promise<any>((resolve, reject) => {
      axios.get<any>(`/api/database/${databaseId}/query/${queryId}/export`, config)
        .then((response) => {
          console.info('Exported query with id', queryId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to export query', error)
          reject(error)
        })
    })
  }

  async function execute(databaseId: number, data: ExecuteStatementDto, page: number | null, size: number | null): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('execute query in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.post<QueryResultDto>(`/api/database/${databaseId}/query`, data, {params: (page && size && { page, size })})
        .then((response) => {
          console.info('Executed query in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to execute query', error)
          reject(error)
        })
    })
  }

  async function reExecuteData(databaseId: number, queryId: number, page: number | null, size: number | null): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('re-execute query in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/query/${queryId}/data`, {params: (page && size && { page, size })})
        .then((response) => {
          console.info('Re-executed query in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to re-execute query', error)
          reject(error)
        })
    })
  }

  async function reExecuteCount(databaseId: number, queryId: number): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('re-execute query in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/query/${queryId}/data/count`)
        .then((response) => {
          console.info('Re-executed query in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to re-execute query', error)
          reject(error)
        })
    })
  }

  function build(table: TableDto, columns: ColumnDto[], clauses: any[]): QueryBuildResultDto {
    var sql = 'SELECT'
    for (let i = 0; i < columns.length; i++) {
      sql += `${i > 0 ? ',' : ''} \`${columns[i].internal_name}\``
    }
    sql += ` FROM \`${table}\``
    if (clauses.length > 0) {
      sql += ' WHERE'
      for (let i = 0; i < clauses.length; i++) {
        const clause = clauses[i]
        if (clause.type === 'and' || clause.type === 'or') {
          sql += ` ${clause.type.toUpperCase()} `
          continue
        }
        const fCol = columns.filter(c => c.internal_name === clause.params[0])
        if (fCol.length === 0) {
          return {
            error: true,
            reason: 'column.exists',
            column: clause.params[0],
            raw: null,
            formatted: null
          }
        }
        sql += ` \`${clause.params[0]}\` ${clause.params[1]} `
        const fCon = mySql8DataTypes().filter(t => t.value === fCol[0].column_type)
        if (fCol.length === 0) {
          return {
            error: true,
            reason: 'type.exists',
            column: fCol[0].column_type,
            raw: null,
            formatted: null
          }
        }
        if (!fCon[0].isBuildable) {
          return {
            error: true,
            reason: 'type.build',
            column: fCol[0].column_type,
            raw: null,
            formatted: null
          }
        }
        if (fCon[0].quoted) {
          sql += `'${clause.params[2]}'`
        } else {
          sql += `${clause.params[2]}`
        }
      }
    }
    return {
      error: false,
      reason: null,
      column: null,
      raw: sql,
      formatted: format(sql, {
        language: 'mysql',
        keywordCase: 'upper'
      })
    }
  }

  function mySql8DataTypes(): MySql8DataType[] {
    return [
      {value: 'bigint', text: 'BIGINT(size)', defaultSize: 255, defaultD: null, quoted: false, isBuildable: true},
      {value: 'binary', text: 'BINARY(size)', defaultSize: 1, defaultD: null, quoted: false, isBuildable: false},
      {value: 'bit', text: 'BIT(size)', defaultSize: 1, defaultD: null, quoted: false, isBuildable: true},
      {value: 'blob', text: 'BLOB', defaultSize: null, defaultD: null, quoted: false, isBuildable: false},
      {value: 'bool', text: 'BOOL', defaultSize: null, defaultD: null, quoted: false, isBuildable: true},
      {value: 'char', text: 'CHAR(size)', defaultSize: 1, defaultD: null, quoted: true, isBuildable: true},
      {value: 'date', text: 'DATE', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'datetime', text: 'DATETIME(fsp)', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'decimal', text: 'DECIMAL(size, d)', defaultSize: 10, defaultD: 4, quoted: false, isBuildable: true},
      {value: 'double', text: 'DOUBLE(size, d)', defaultSize: 25, defaultD: 4, quoted: false, isBuildable: true},
      {value: 'enum', text: 'ENUM(val1,val2,...)', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'float', text: 'FLOAT(p)', defaultSize: 24, defaultD: null, quoted: false, isBuildable: true},
      {value: 'int', text: 'INT(size)', defaultSize: 255, defaultD: null, quoted: false, isBuildable: true},
      {value: 'longblob', text: 'LONGBLOB', defaultSize: null, defaultD: null, quoted: false, isBuildable: false},
      {value: 'longtext', text: 'LONGTEXT', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'mediumblob', text: 'MEDIUMBLOB', defaultSize: null, defaultD: null, quoted: false, isBuildable: false},
      {value: 'mediumint', text: 'MEDIUMINT(size)', defaultSize: 10, defaultD: null, quoted: false, isBuildable: true},
      {value: 'mediumtext', text: 'MEDIUMTEXT', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'set', text: 'SET(val1,val2,...)', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'smallint', text: 'SMALLINT(size)', defaultSize: 10, defaultD: null, quoted: false, isBuildable: true},
      {value: 'text', text: 'TEXT', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'time', text: 'TIME(fsp)', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'timestamp', text: 'TIMESTAMP(fsp)', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'tinyblob', text: 'TINYBLOB', defaultSize: null, defaultD: null, quoted: false, isBuildable: false},
      {value: 'tinyint', text: 'TINYINT(size)', defaultSize: 10, defaultD: null, quoted: false, isBuildable: true},
      {value: 'tinytext', text: 'TINYTEXT', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'year', text: 'YEAR', defaultSize: null, defaultD: null, quoted: true, isBuildable: true},
      {value: 'varbinary', text: 'VARBINARY(size)', defaultSize: 1, defaultD: null, quoted: false, isBuildable: false},
      {value: 'varchar', text: 'VARCHAR(size)', defaultSize: 255, defaultD: null, quoted: true, isBuildable: true}
    ]
  }

  return {findAll, findOne, update, exportCsv, execute, reExecuteData, reExecuteCount, build, mySql8DataTypes}
}
