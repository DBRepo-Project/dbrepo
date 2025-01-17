import {axiosErrorToApiError} from '@/utils'

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
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function fuzzy_search(term: string): Promise<DatabaseDto[]> {
    const axios = useAxiosInstance()
    console.debug('fuzzy search for term', term)
    return new Promise<DatabaseDto[]>((resolve, reject) => {
      axios.get<DatabaseDto[]>(`/api/search?q=${term}`)
        .then((response) => {
          console.info('Searched for term', term)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to search', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function general_search(type: string, data: any): Promise<SearchResultDto> {
    const axios = useAxiosInstance()
    console.debug('general search for type', type)
    return new Promise<SearchResultDto>((resolve, reject) => {
      axios.post<SearchResultDto>(`/api/search/${type}`, data)
        .then((response) => {
          console.info('Searched for type', type)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to search', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  return {fields, fuzzy_search, general_search}
}
