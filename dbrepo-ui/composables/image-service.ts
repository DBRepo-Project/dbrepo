import {axiosErrorToApiError} from '@/utils'

export const useImageService = (): any => {
  async function findById(id: number): Promise<ImageDto> {
    const axios = useAxiosInstance();
    console.debug('find image by id', id);
    return new Promise<ImageDto>((resolve, reject) => {
      axios.get<ImageDto>(`/api/image/${id}`)
        .then((response) => {
          console.info('Found image')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find image', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {findById}
}
