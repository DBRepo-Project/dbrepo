import {axiosErrorToApiError} from '@/utils'

export const useAnalyseService = (): any => {
  async function suggest (data: DetermineDataTypesDto): Promise<DataTypesDto[]> {
    const axios = useAxiosInstance()
    console.debug('suggest data types for columns')
    return new Promise<DataTypesDto[]>((resolve, reject) => {
      axios.get<DataTypesDto[]>('/api/analyse/datatypes', { params: data })
        .then((response) => {
          console.info('Suggested data types for column(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to suggest data types for columns', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {suggest}
}
