import {axiosErrorToApiError} from '@/utils'

export const useAccessService = (): any => {
  async function findOne(databaseId: number, userId: string): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('find access of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.get<DatabaseAccessDto>(`/api/database/${databaseId}/access/${userId}`)
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

  async function create(databaseId: number, userId: number, payload: DatabaseGiveAccessDto): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('create access for user with id', userId, 'of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.post<DatabaseAccessDto>(`/api/database/${databaseId}/access/${userId}`, payload)
        .then((response) => {
          console.info('Created access for user with id', userId, 'of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create access', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function update(databaseId: number, userId: number, payload: DatabaseModifyAccessDto): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('update access for user with id', userId, 'of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.put<DatabaseAccessDto>(`/api/database/${databaseId}/access/${userId}`, payload)
        .then((response) => {
          console.info('Updated access for user with id', userId, 'of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update access', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function remove(databaseId: number, userId: number): Promise<DatabaseAccessDto> {
    const axios = useAxiosInstance()
    console.debug('remove access for user with id', userId, 'of database with id', databaseId)
    return new Promise<DatabaseAccessDto>((resolve, reject) => {
      axios.delete<DatabaseAccessDto>(`/api/database/${databaseId}/access/${userId}`)
        .then((response) => {
          console.info('Removed access for user with id', userId, 'of database with id', databaseId)
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
