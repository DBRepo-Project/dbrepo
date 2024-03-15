export const useAnalyseService = (): any => {
  async function suggest (data: DetermineDataTypesDto): Promise<DataTypesDto[]> {
    const axios = useAxiosInstance()
    console.debug('suggest data types for columns')
    return new Promise<DataTypesDto[]>((resolve, reject) => {
      axios.post<DataTypesDto[]>('/api/analyse/determinedt', data)
        .then((response) => {
          console.info('Suggested data types for column(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to suggest data types for columns', error)
          reject(error)
        })
    })
  }

  return {suggest}
}
