import {format} from 'sql-formatter'
import type {AxiosRequestConfig} from 'axios'
import {axiosErrorToApiError} from '@/utils'

export const useQueryService = (): any => {
  async function findAll(databaseId: string, persisted: boolean): Promise<QueryDto[]> {
    const axios = useAxiosInstance()
    console.debug('find queries')
    return new Promise<QueryDto[]>((resolve, reject) => {
      axios.get<QueryDto[]>(`/api/database/${databaseId}/subset`, {params: (persisted && {persisted})})
        .then((response) => {
          console.info(`Found ${response.data.length} query(s)`)
          resolve(response.data)
        })
        .catch((error) => {
          if (error.response.status === 403) {
            /* ignore */
            resolve([])
          }
          console.error('Failed to find queries', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function findOne(databaseId: string, queryId: string): Promise<QueryDto> {
    const axios = useAxiosInstance()
    console.debug('find query with id', queryId, 'in database with id', databaseId)
    return new Promise<QueryDto>((resolve, reject) => {
      axios.get<QueryDto>(`/api/database/${databaseId}/subset/${queryId}`)
        .then((response) => {
          console.info('Found query with id', queryId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find query', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function update(databaseId: string, queryId: string, data: QueryPersistDto): Promise<QueryDto> {
    const axios = useAxiosInstance()
    console.debug('update query with id', queryId, 'in database with id', databaseId)
    return new Promise<QueryDto>((resolve, reject) => {
      axios.put<QueryDto>(`/api/database/${databaseId}/subset/${queryId}`, data)
        .then((response) => {
          console.info('Updated query with id', queryId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update query', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function exportCsv(databaseId: string, queryId: string): Promise<any> {
    const axios = useAxiosInstance()
    const config: AxiosRequestConfig = {
      responseType: 'blob',
      headers: {
        Accept: 'text/csv'
      }
    }
    console.debug('export query with id', queryId, 'in database with id', databaseId)
    return new Promise<any>((resolve, reject) => {
      axios.get<any>(`/api/database/${databaseId}/subset/${queryId}`, config)
        .then((response) => {
          console.info('Exported query with id', queryId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to export query', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function execute(databaseId: string, data: ExecuteStatementDto, timestamp: Date | null, page: number, size: number): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('execute query in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.post<QueryResultDto>(`/api/database/${databaseId}/subset`, data, {params: mapFilter(timestamp, page, size), timeout: 600_000})
        .then((response) => {
          console.info('Executed query with id', response.data.id, ' in database with id', databaseId)
          const result: QueryResultDto = {
            id: response.headers['x-id'],
            headers: [],
            result: response.data
          }
          resolve(result)
        })
        .catch((error) => {
          console.error('Failed to execute query', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function reExecuteData(databaseId: string, queryId: string, page: number, size: number): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('re-execute query in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/subset/${queryId}/data`, { params: mapFilter(null, page, size) })
        .then((response) => {
          console.info('Re-executed query in database with id', databaseId)
          const result: QueryResultDto = {
            id: response.headers['x-id'],
            headers: response.headers['x-headers'] ? response.headers['x-headers'].split(',') : [],
            result: response.data
          }
          resolve(result)
        })
        .catch((error) => {
          console.error('Failed to re-execute query', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function reExecuteCount(databaseId: string, queryId: string): Promise<number> {
    const axios = useAxiosInstance()
    console.debug('re-execute query in database with id', databaseId)
    return new Promise<number>((resolve, reject) => {
      axios.head<void>(`/api/database/${databaseId}/subset/${queryId}/data`)
        .then((response) => {
          const count: number = Number(response.headers['x-count'])
          console.info('Found', count, 'tuples for query', queryId, 'in database with id', databaseId)
          resolve(count)
        })
        .catch((error) => {
          console.error('Failed to re-execute query', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  function build(table: TableDto, columns: ColumnDto[], types: DataTypeDto[], clauses: any[]): QueryBuildResultDto {
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
        const filteredColumn = columns.filter(c => c.internal_name === clause.params[0])
        if (filteredColumn.length === 0) {
          return {
            error: true,
            reason: 'column.exists',
            column: clause.params[0],
            raw: null,
            formatted: null
          }
        }
        sql += ` \`${clause.params[0]}\` ${clause.params[1]} `
        const filteredType = types.filter(t => t.value === filteredColumn[0].type)
        if (filteredType.length === 0) {
          return {
            error: true,
            reason: 'exists',
            column: filteredColumn[0].type,
            raw: null,
            formatted: null
          }
        }
        if (!filteredType[0].is_buildable) {
          return {
            error: true,
            reason: 'build',
            column: filteredColumn[0].type,
            raw: null,
            formatted: null
          }
        }
        if (filteredType[0].is_quoted) {
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

  function mapFilter(timestamp: Date | null, page: number, size: number) {
    if (!timestamp) {
      return {page, size}
    }
    return {timestamp, page, size}
  }

  return {findAll, findOne, update, exportCsv, execute, reExecuteData, reExecuteCount, build}
}
