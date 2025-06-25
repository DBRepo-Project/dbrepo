import {axiosErrorToApiError} from '@/utils'

export const useAnalyseService = (): any => {
  async function determineSchema (s3key: string): Promise<SchemaAnalysisResultDto> {
    const axios = useAxiosInstance()
    console.debug('suggest data types for columns')
    return new Promise<SchemaAnalysisResultDto>((resolve, reject) => {
      axios.get<SchemaAnalysisResultDto>('/api/analyse/schema/' + s3key)
        .then((response) => {
          console.info('Determined schema for dataset')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to determine schema for dataset', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {determineSchema}
}
