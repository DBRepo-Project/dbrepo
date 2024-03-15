export const useConceptService = (): any => {
  async function findAll () {
    const axios = useAxiosInstance()
    return new Promise((resolve, reject) => {
      axios.get('/api/semantic/concept')
        .then((response) => {
          console.info('Found concept(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find concepts', error)
          reject(error)
        })
    })
  }

  function mapConcepts (data: ConceptDto[]): ConceptDto[] {
    return data.map((concept) => {
      concept.name = concept.name ? concept.name : concept.uri
      return concept
    })
  }

  return {findAll, mapConcepts}
}
