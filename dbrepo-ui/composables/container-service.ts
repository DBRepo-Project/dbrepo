import {axiosErrorToApiError} from '@/utils'

export const useContainerService = (): any => {
  async function findAll(): Promise<ContainerBriefDto[]> {
    const axios = useAxiosInstance();
    console.debug('find containers');
    return new Promise<ContainerBriefDto[]>((resolve, reject) => {
      axios.get<ContainerBriefDto[]>('/api/v1/container')
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

  async function findOne(containerId: string): Promise<ContainerDto> {
    const axios = useAxiosInstance();
    console.debug('find containers');
    return new Promise<ContainerDto>((resolve, reject) => {
      axios.get<ContainerDto>(`/api/v1/container/${containerId}`)
        .then((response) => {
          console.info(`Find container with id ${containerId}`)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find container', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findAll, findOne}
}
