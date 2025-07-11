import {axiosErrorToApiError} from '@/utils'

export const useGrantService = (): any => {
  async function findOne(databaseId: string, username: string): Promise<DatabaseGrantsDto> {
    const axios = useAxiosInstance()
    console.debug('find grant of database with id', databaseId)
    return new Promise<DatabaseGrantsDto>((resolve, reject) => {
      axios.get<DatabaseGrantsDto>(`/api/database/${databaseId}/grant/${username}`)
        .then((response) => {
          console.info('Found grant of database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find grant', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findOne}
}
