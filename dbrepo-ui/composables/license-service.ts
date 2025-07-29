import {axiosErrorToApiError} from '@/utils'

export const useLicenseService = (): any => {
  async function findAll(): Promise<LicenseDto[]> {
    const axios = useAxiosInstance()
    console.debug('find licenses')
    return new Promise<LicenseDto[]>((resolve, reject) => {
      axios.get<LicenseDto[]>('/api/v1/license')
        .then((response) => {
          console.info('Found license(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find licenses')
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findAll}
}
