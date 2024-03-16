export const useOntologyService = (): any => {
  async function findAll(): Promise<OntologyDto[]> {
    const axios = useAxiosInstance()
    console.debug('find ontologies')
    return new Promise<OntologyDto[]>((resolve, reject) => {
      axios.get<OntologyDto[]>('/api/semantic/ontology')
        .then((response) => {
          console.info(`Found ${response.data.length} ontology(s)`)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find ontologies', error)
          reject(error)
        })
    })
  }

  async function findOne(id: number): Promise<OntologyDto> {
    const axios = useAxiosInstance()
    console.debug('find ontology for id', id)
    return new Promise<OntologyDto>((resolve, reject) => {
      axios.get<OntologyDto>(`/api/semantic/ontology/${id}`)
        .then((response) => {
          console.info('Found ontology for id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find ontology', error)
          reject(error)
        })
    })
  }

  async function create(data: OntologyCreateDto): Promise<OntologyDto> {
    const axios = useAxiosInstance()
    console.debug('create ontology')
    return new Promise<OntologyDto>((resolve, reject) => {
      axios.post<OntologyDto>('/api/semantic/ontology', data)
        .then((response) => {
          console.info('Created ontology with id', response.data.id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create ontology', error)
          reject(error)
        })
    })
  }

  async function update(id: number, data: OntologyModifyDto): Promise<OntologyDto> {
    const axios = useAxiosInstance()
    console.debug('update ontology with id', id)
    return new Promise<OntologyDto>((resolve, reject) => {
      axios.put<OntologyDto>(`/api/semantic/ontology/${id}`, data)
        .then((response) => {
          console.info('Updated ontology with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to update ontology', error)
          reject(error)
        })
    })
  }

  async function remove(id: number): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete ontology with id', id)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/semantic/ontology/${id}`)
        .then(() => {
          console.info('Deleted ontology with id', id)
          resolve()
        })
        .catch((error) => {
          console.error('Failed to delete ontology', error)
          reject(error)
        })
    })
  }

  return {findAll, findOne, create, update, remove}
}
