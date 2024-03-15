export const useViewService = (): any => {
  async function remove(databaseId: number, viewId: number): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete view with id', viewId, 'in database with id', databaseId)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/database/${databaseId}/view/${viewId}`)
        .then((response) => {
          console.info('Deleted view with id', viewId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to delete view', error)
          reject(error)
        })
    })
  }

  async function create(databaseId: number, payload: ViewCreateDto): Promise<ViewDto> {
    const axios = useAxiosInstance()
    console.debug('create view in database with id', databaseId)
    return new Promise<ViewDto>((resolve, reject) => {
      axios.post<ViewDto>(`/api/database/${databaseId}/view`, payload)
        .then((response) => {
          console.info('Created view in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create view', error)
          reject(error)
        })
    })
  }

  async function reExecuteData(databaseId: number, viewId: number, page: number | null, size: number | null): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('re-execute view with id', viewId, 'in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/view/${viewId}/data`, {params: {page, size}})
        .then((response) => {
          console.info('Re-executed view with id', viewId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to re-execute view', error)
          reject(error)
        })
    })
  }

  async function reExecuteCount(databaseId: number, viewId: number): Promise<QueryResultDto> {
    const axios = useAxiosInstance()
    console.debug('re-execute view with id', viewId, 'in database with id', databaseId)
    return new Promise<QueryResultDto>((resolve, reject) => {
      axios.get<QueryResultDto>(`/api/database/${databaseId}/view/${viewId}/data/count`)
        .then((response) => {
          console.info('Re-executed view with id', viewId, 'in database with id', databaseId)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to re-execute view', error)
          reject(error)
        })
    })
  }

  return {remove, create, reExecuteData, reExecuteCount}
}
