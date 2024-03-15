export const useSearchService = (): any => {
  async function fields(type: string): Promise<FieldsResultDto[]> {
    const axios = useAxiosInstance()
    console.debug('find fields for type', type)
    return new Promise<FieldsResultDto[]>((resolve, reject) => {
      axios.get<FieldsResultDto[]>(`/api/search/${type}/fields`)
        .then((response) => {
          const json = response.data
          console.debug('Found fields for type', type)
          resolve(json)
        })
        .catch((error) => {
          console.error('Failed to find fields', error)
          reject(error)
        })
    })
  }

  async function search(type: string, data: SearchDto): Promise<SearchResultDto> {
    const axios = useAxiosInstance()
    console.debug('search for type', type)
    return new Promise<SearchResultDto>((resolve, reject) => {
      axios.post<SearchResultDto>(`/api/search${type ? `/${type}` : ''}`, data)
        .then((response) => {
          console.info('Searched for type', type)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to search', error)
          reject(error)
        })
    })
  }

  return {fields, search}
}
