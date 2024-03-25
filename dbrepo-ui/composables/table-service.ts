import type {AxiosRequestConfig} from 'axios'

export const useTableService = (): any => {

  function findAll(databaseId: number): Promise<TableBriefDto> {
    const axios = useAxiosInstance()
    console.debug('find tables')
    return new Promise<TableBriefDto>((resolve, reject) => {
      axios.get<TableBriefDto>(`/api/database/${databaseId}/table`)
        .then((response) => {
          console.info('Found tables(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find tables', error)
          reject(error)
        })
    })
  }

  async function findOne(databaseId: number, tableId: number): Promise<TableDto> {
    const axios = useAxiosInstance()
    console.debug('find table with id', tableId, 'in database with id', databaseId);
    return new Promise<TableDto>((resolve, reject) => {
      axios.get<TableDto>(`/api/database/${databaseId}/table/${tableId}`)
        .then((response) => {
          console.info('Found table with id', tableId, 'in database with id', databaseId);
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find table with id', tableId, 'in database with id', databaseId)
          reject(error)
        })
    })
  }

  async function update(databaseId: number, tableId: number, columnId: number, data: ColumnSemanticsUpdateDto): Promise<ColumnDto> {
    const axios = useAxiosInstance()
    console.debug('update column with id', columnId, 'table with id', tableId, 'in database with id', databaseId);
    return new Promise<ColumnDto>((resolve, reject) => {
      axios.put<ColumnDto>(`/api/database/${databaseId}/table/${tableId}/column/${columnId}`, data)
        .then((response) => {
          console.info('Updated column with id', columnId, 'table with id', tableId, 'in database with id', databaseId);
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update column with id', columnId, 'table with id', tableId, 'in database with id', databaseId)
          reject(error)
        })
    })
  }

  async function importCsv(databaseId: number, tableId: number, data: ImportCsv): Promise<ImportDto> {
    const axios = useAxiosInstance()
    console.debug('import csv to table with id', tableId, 'in database with id', databaseId);
    return new Promise<ImportDto>((resolve, reject) => {
      axios.post<ImportDto>(`/api/database/${databaseId}/table/${tableId}/data/import`, data)
        .then((response) => {
          console.info('Imported csv to table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to import csv', error)
          reject(error)
        })
    })
  }

  async function getData(databaseId: number, tableId: number, page: number, size: number, timestamp: Date): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('get data for table with id', tableId, 'in database with id', databaseId);
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/table/${tableId}/data`, {params: mapFilter(timestamp, page, size)})
        .then((response) => {
          console.info('Got data for table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to get data')
          reject(error)
        })
    })
  }

  async function getCount(databaseId: number, tableId: number, timestamp: Date): Promise<number> {
    const axios = useAxiosInstance()
    console.debug('get data count for table with id', tableId, 'in database with id', databaseId);
    return new Promise<number>((resolve, reject) => {
      axios.head<number>(`/api/database/${databaseId}/table/${tableId}/data`, {params: mapFilter(timestamp, null, null)})
        .then((response) => {
          console.info('Got data count for table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to get data count')
          reject(error)
        })
    })
  }

  async function exportData(databaseId: number, tableId: number, timestamp: Date): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    const config: AxiosRequestConfig = {
      params: (timestamp && { timestamp }),
      responseType: 'blob',
      headers: {
        Accept: 'text/csv'
      }
    }
    console.debug('export data for table with id', tableId, 'in database with id', databaseId);
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/table/${tableId}/export`, config)
        .then((response) => {
          console.info('Exported data for table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to export data')
          reject(error)
        })
    })
  }

  async function create(databaseId: number, data: TableCreateDto): Promise<TableDto> {
    const axios = useAxiosInstance()
    console.debug('create table in database with id', databaseId)
    return new Promise<TableDto>((resolve, reject) => {
      axios.post<TableDto>(`/api/database/${databaseId}/table`, data)
        .then((response) => {
          console.info('Created table in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create table in database with id', databaseId)
          reject(error)
        })
    });
  }

  async function remove(databaseId: number, tableId: number): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete table with id', tableId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/database/${databaseId}/table/${tableId}`)
        .then((response) => {
          console.info('Deleted table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete table', error)
          reject(error)
        })
    });
  }

  async function removeTuple(databaseId: number, tableId: number, data: TableCsvDeleteDto): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete tuple(s) in table with id', tableId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/database/${databaseId}/table/${tableId}`, {data})
        .then((response) => {
          console.info('Deleted tuple(s) in table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete tuple(s)', error)
          reject(error)
        })
    });
  }

  async function history(databaseId: number, tableId: number): Promise<TableHistoryDto[]> {
    const axios = useAxiosInstance()
    console.debug('Load history of table with id', tableId, 'in database with id', databaseId)
    return new Promise<TableHistoryDto[]>((resolve, reject) => {
      axios.get<TableHistoryDto[]>(`/api/database/${databaseId}/table/${tableId}/history`)
        .then((response) => {
          console.info('Loaded history of table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to load history', error)
          reject(error)
        })
    });
  }

  async function suggest(databaseId: number, tableId: number, columnId: number): Promise<TableColumnEntityDto[]> {
    const axios = useAxiosInstance()
    console.debug('suggest semantic entities for table column with id', columnId, 'of table with id', tableId, 'of database with id', databaseId)
    return new Promise<TableColumnEntityDto[]>((resolve, reject) => {
      axios.get<TableColumnEntityDto[]>(`/api/semantic/database/${databaseId}/table/${tableId}/column/${columnId}`, { timeout: 10000 })
        .then((response) => {
          console.info('Suggested semantic entities for table column with id', columnId, 'of table with id', tableId, 'of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to suggest semantic entities', error)
          reject(error)
        })
    })
  }

  function isOwner(table: TableDto, user: UserDto) {
    if (!table || !user) {
      return false
    }
    return table.owner.id === user.id
  }

  function tableNameToInternalName(name: string) {
    return name.normalize('NFKD')
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^\w-]+/g, '_')
      .replace(/--+/g, '_')
  }

  function mapFilter(timestamp: Date | null, page: number | null, size: number | null) {
    if (!timestamp) {
      if (!page || !size) {
        return null
      }
      return {page, size}
    }
    if (!page || !size) {
      return {timestamp}
    }
  }

  return {
    findAll,
    findOne,
    update,
    importCsv,
    getData,
    getCount,
    exportData,
    create,
    remove,
    removeTuple,
    history,
    suggest,
    isOwner,
    tableNameToInternalName
  }
}
