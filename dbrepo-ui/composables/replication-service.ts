import {axiosErrorToApiError} from '@/utils'

export const useReplicationService = (): any => {
  const request = async <T>(operation: () => Promise<{ data: T }>): Promise<T> => {
    try {
      return (await operation()).data
    } catch (error) {
      throw axiosErrorToApiError(error)
    }
  }

  async function findStatus (): Promise<ReplicationStatusDto> {
    const axios = useAxiosInstance()
    return request(() => axios.get<ReplicationStatusDto>('/api/replication/status'))
  }

  async function findReplicationOutbox (): Promise<ReplicationOutboxEntryDto[]> {
    const axios = useAxiosInstance()
    return request(() => axios.get<ReplicationOutboxEntryDto[]>('/api/replication/outbox'))
  }

  async function retryReplicationOutbox (): Promise<ReplicationRetryResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<ReplicationRetryResultDto>('/api/replication/outbox/retry'))
  }

  async function retryReplicationOutboxEntry (id: string): Promise<ReplicationRetryResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<ReplicationRetryResultDto>(`/api/replication/outbox/${id}/retry`))
  }

  async function findMetadataOutbox (): Promise<MetadataReplicationOutboxEntryDto[]> {
    const axios = useAxiosInstance()
    return request(() => axios.get<MetadataReplicationOutboxEntryDto[]>('/api/metadata/replication/outbox'))
  }

  async function retryMetadataOutbox (): Promise<ReplicationRetryResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<ReplicationRetryResultDto>('/api/metadata/replication/outbox/retry'))
  }

  async function retryMetadataOutboxEntry (id: string): Promise<ReplicationRetryResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<ReplicationRetryResultDto>(`/api/metadata/replication/outbox/${id}/retry`))
  }

  async function findDataOutbox (databaseId: string): Promise<DataReplicationOutboxEntryDto[]> {
    const axios = useAxiosInstance()
    return request(() => axios.get<DataReplicationOutboxEntryDto[]>(`/api/v1/database/${databaseId}/replication/outbox`))
  }

  async function retryDataOutbox (databaseId: string): Promise<ReplicationRetryResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<ReplicationRetryResultDto>(`/api/v1/database/${databaseId}/replication/outbox/retry`))
  }

  async function retryDataOutboxEntry (databaseId: string, id: string): Promise<ReplicationRetryResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<ReplicationRetryResultDto>(`/api/v1/database/${databaseId}/replication/outbox/${id}/retry`))
  }

  async function synchroniseDatabase (databaseId: string, pageSize: number): Promise<DatabaseSynchronisationResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<DatabaseSynchronisationResultDto>(
      `/api/replication/data/synchronise/database/${databaseId}`,
      null,
      {params: {pageSize}}
    ))
  }

  async function synchroniseTable (databaseId: string, tableId: string,
                                   pageSize: number): Promise<TableSynchronisationResultDto> {
    const axios = useAxiosInstance()
    return request(() => axios.post<TableSynchronisationResultDto>(
      `/api/replication/data/synchronise/database/${databaseId}/table/${tableId}`,
      null,
      {params: {pageSize}}
    ))
  }

  async function findAccess (): Promise<ReplicationAccessDto[]> {
    const axios = useAxiosInstance()
    return request(() => axios.get<ReplicationAccessDto[]>('/api/v1/database/replication-access'))
  }

  async function mapAccess (databaseId: string, localUsername: string): Promise<ReplicationAccessDto> {
    const axios = useAxiosInstance()
    return request(() => axios.put<ReplicationAccessDto>(
      `/api/v1/database/${databaseId}/replication-access`,
      {local_username: localUsername}
    ))
  }

  return {
    findStatus,
    findReplicationOutbox,
    retryReplicationOutbox,
    retryReplicationOutboxEntry,
    findMetadataOutbox,
    retryMetadataOutbox,
    retryMetadataOutboxEntry,
    findDataOutbox,
    retryDataOutbox,
    retryDataOutboxEntry,
    synchroniseDatabase,
    synchroniseTable,
    findAccess,
    mapAccess
  }
}
