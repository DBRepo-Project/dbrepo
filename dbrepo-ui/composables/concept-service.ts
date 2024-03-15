export const useConceptService = (): any => {
  async function findAll () {
    const axios = useAxiosInstance()
    return new Promise((resolve, reject) => {
      axios.get('/api/semantic/concept', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const concepts = response.data
          console.debug('response concepts', concepts)
          resolve(concepts)
        })
        .catch((error) => {
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
