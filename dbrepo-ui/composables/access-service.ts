import {axiosErrorToApiError} from '@/utils'

export const useAccessService = (): any => {
  async function findOne(databaseId: string, username: string): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('find access of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.get<DatabaseAccessDto>(`/api/v1/database/${databaseId}/access/${username}`)
        .then((response) => {
          console.info('Found access of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find access', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function create(databaseId: string, username: string, payload: DatabaseGiveAccessDto): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('create access for user with id', username, 'of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.post<DatabaseAccessDto>(`/api/v1/database/${databaseId}/access/${username}`, payload)
        .then((response) => {
          console.info('Created access for user', username, 'of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create access', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function update(databaseId: string, username: string, payload: DatabaseModifyAccessDto): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('update access for user', username, 'of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.put<DatabaseAccessDto>(`/api/v1/database/${databaseId}/access/${username}`, payload)
        .then((response) => {
          console.info('Updated access for user', username, 'of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update access', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function remove(databaseId: string, username: string): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('remove access for user', username, 'of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.delete<DatabaseAccessDto>(`/api/v1/database/${databaseId}/access/${username}`)
        .then((response) => {
          console.info('Removed access for user', username, 'of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to remove access', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findOne, create, update, remove}
}
