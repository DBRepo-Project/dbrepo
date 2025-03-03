import {axiosErrorToApiError} from '@/utils'

export const useTupleService = (): any => {
  async function create(databaseId: string, tableId: string, data: TableCsvDto): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('create tuple(s) in table with id', tableId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.post<void>(`/api/database/${databaseId}/table/${tableId}/data`, data)
        .then((response) => {
          console.info('Created tuple(s) in table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create tuple(s)', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function update(databaseId: string, tableId: string, data: TableCsvDto): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('update tuple(s) in table with id', tableId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.put<void>(`/api/database/${databaseId}/table/${tableId}/data`, data)
        .then((response) => {
          console.info('Updated tuple(s) in table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update tuple(s)', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function remove(databaseId: string, tableId: string, data: TableCsvDeleteDto): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete tuple(s) in table with id', tableId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/database/${databaseId}/table/${tableId}/data`, { data })
        .then((response) => {
          console.info('Deleted tuple(s) in table with id', tableId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete tuple(s)', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {create, update, remove}
}
