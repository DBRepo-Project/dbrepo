import {axiosErrorToApiError} from '@/utils'
import type {AxiosRequestConfig} from "axios";

export const useViewService = (): any => {
  async function remove(databaseId: string, viewId: string): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete view with id', viewId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/database/${databaseId}/view/${viewId}`)
        .then((response) => {
          console.info('Deleted view with id', viewId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete view', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function findOne(databaseId: string, viewId: string): Promise<ViewDto> {
    const axios = useAxiosInstance()
    console.debug('get view with id', viewId, 'in database with id', databaseId)
    return new Promise<ViewDto>((resolve, reject) => {
      axios.get<ViewDto>(`/api/database/${databaseId}/view/${viewId}`)
        .then((response) => {
          console.info('Found view with id', viewId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete view', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function create(databaseId: string, payload: ViewCreateDto): Promise<ViewDto> {
    const axios = useAxiosInstance()
    console.debug('create view in database with id', databaseId)
    return new Promise<ViewDto>((resolve, reject) => {
      axios.post<ViewDto>(`/api/database/${databaseId}/view`, payload)
        .then((response) => {
          console.info('Created view in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create view', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function update(databaseId: string, viewId: string, payload: ViewUpdateDto): Promise<ViewDto> {
    const axios = useAxiosInstance()
    console.debug('update view with id', viewId)
    return new Promise<ViewDto>((resolve, reject) => {
      axios.put<ViewDto>(`/api/database/${databaseId}/view/${viewId}`, payload)
        .then((response) => {
          console.info('Update view with id', viewId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update view', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function reExecuteData(databaseId: string, viewId: string, page: number | null, size: number | null): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('re-execute view with id', viewId, 'in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/view/${viewId}/data`, {params: {page, size}})
        .then((response) => {
          console.info('Re-executed view with id', viewId, 'in database with id', databaseId)
          const result: QueryResultDto = {
            id: viewId,
            headers: response.headers['x-headers'] ? response.headers['x-headers'].split(',') : [],
            result: response.data
          }
          resolve(result)
        })
        .catch((error) => {
          console.error('Failed to re-execute view', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function reExecuteCount(databaseId: string, viewId: string): Promise<number> {
    const axios = useAxiosInstance()
    console.debug('re-execute view with id', viewId, 'in database with id', databaseId)
    return new Promise<number>((resolve, reject) => {
      axios.head<number>(`/api/database/${databaseId}/view/${viewId}/data`)
        .then((response) => {
          const count: number = Number(response.headers['x-count'])
          console.info('Found', count, 'tuples for view with id', viewId, 'in database with id', databaseId)
          resolve(count)
        })
        .catch((error) => {
          console.error('Failed to re-execute view', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function exportData(databaseId: string, viewId: string): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    const config: AxiosRequestConfig = {
      responseType: 'blob',
      headers: {
        Accept: 'text/csv'
      }
    }
    console.debug('export data for view with id', viewId, 'in database with id', databaseId);
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/view/${viewId}/data`, config)
        .then((response) => {
          console.info('Exported data for view with id', viewId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to export data', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {remove, findOne, create, update, reExecuteData, reExecuteCount, exportData}
}
