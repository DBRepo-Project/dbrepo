import {axiosErrorToApiError} from '@/utils'

export const useContainerService = (): any => {
  async function findAll(): Promise<ContainerBriefDto[]> {
    const axios = useAxiosInstance();
    console.debug('find containers');
    return new Promise<ContainerBriefDto[]>((resolve, reject) => {
      axios.get<ContainerBriefDto[]>('/api/container')
        .then((response) => {
          console.info(`Found ${response.data.length} container(s)`)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find containers', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findAll}
}
