import Vue from 'vue'
import api from '@/api'

class SemanticService {
  findAllOntologies () {
    return new Promise((resolve, reject) => {
      api.get('http://localhost/api/semantic/ontology', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const ontologies = response.data
          console.debug('response ontologies', ontologies)
          resolve(ontologies)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load ontologies', error)
          Vue.$toast.error(`[${code}] Failed to load ontologies: ${message}`)
          reject(error)
        })
    })
  }
}

export default new SemanticService()
