export const useMessageService = (): any => {
  async function findAll(filter: string | null): Promise<BannerMessageDto[]> {
    const axios = useAxiosInstance()
    console.debug('find messages')
    return new Promise<BannerMessageDto[]>((resolve, reject) => {
      axios.get<BannerMessageDto[]>(`/api/maintenance/message`, {params: (filter && { filter })})
        .then((response) => {
          console.info('Found message(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find messages', error)
          reject(error)
        })
    })
  }

  async function findOne(id: number): Promise<BannerMessageDto> {
    const axios = useAxiosInstance()
    console.debug('find message with id', id)
    return new Promise<BannerMessageDto>((resolve, reject) => {
      axios.get<BannerMessageDto>(`/api/maintenance/message/${id}`)
        .then((response) => {
          console.info('Found message with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find message', error)
          reject(error)
        })
    })
  }

  async function create(data: BannerMessageCreateDto): Promise<BannerMessageDto> {
    const axios = useAxiosInstance()
    console.debug('create message')
    return new Promise<BannerMessageDto>((resolve, reject) => {
      axios.post<BannerMessageDto>('/api/maintenance/message', data)
        .then((response) => {
          console.info('Create message')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create message', error)
          reject(error)
        })
    })
  }

  async function update(id: number, data: BannerMessageUpdateDto): Promise<BannerMessageDto> {
    const axios = useAxiosInstance()
    console.debug('update message with id', id)
    return new Promise<BannerMessageDto>((resolve, reject) => {
      axios.post<BannerMessageDto>(`/api/maintenance/message/${id}`, data)
        .then((response) => {
          console.info('Update message with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update message', error)
          reject(error)
        })
    })
  }

  async function remove(id: number): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete message with id', id)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/maintenance/message/${id}`)
        .then((response) => {
          console.info('Deleted message with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete message', error)
          reject(error)
        })
    })
  }

  return {findAll, findOne, create, update, remove}
}
