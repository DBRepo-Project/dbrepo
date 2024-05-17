import {axiosErrorToApiError} from '@/utils'

export const useMessageService = (): any => {
  async function findAll(filter: string | null): Promise<BannerMessageDto[]> {
    const axios = useAxiosInstance()
    console.debug('find messages')
    return new Promise<BannerMessageDto[]>((resolve, reject) => {
      axios.get<BannerMessageDto[]>(`/api/message`, {params: (filter && { filter })})
        .then((response) => {
          console.info('Found message(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find messages', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function findOne(id: number): Promise<BannerMessageDto> {
    const axios = useAxiosInstance()
    console.debug('find message with id', id)
    return new Promise<BannerMessageDto>((resolve, reject) => {
      axios.get<BannerMessageDto>(`/api/message/${id}`)
        .then((response) => {
          console.info('Found message with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find message', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function create(data: BannerMessageCreateDto): Promise<BannerMessageDto> {
    const axios = useAxiosInstance()
    console.debug('create message')
    return new Promise<BannerMessageDto>((resolve, reject) => {
      axios.post<BannerMessageDto>('/api/message', data)
        .then((response) => {
          console.info('Create message')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create message', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function update(id: number, data: BannerMessageUpdateDto): Promise<BannerMessageDto> {
    const axios = useAxiosInstance()
    console.debug('update message with id', id)
    return new Promise<BannerMessageDto>((resolve, reject) => {
      axios.post<BannerMessageDto>(`/api/message/${id}`, data)
        .then((response) => {
          console.info('Update message with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update message', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function remove(id: number): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete message with id', id)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/message/${id}`)
        .then((response) => {
          console.info('Deleted message with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete message', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findAll, findOne, create, update, remove}
}
