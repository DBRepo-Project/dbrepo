import Vue from 'vue'
import axios from 'axios'

class SearchService {
  getFields (type) {
    return new Promise((resolve, reject) => {
      axios.get(`/api/search/${type}/fields`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const jsonResponse = response.data
          resolve(jsonResponse)
        })
        .catch((error) => {
          const { code, message } = error
          console.error(`Failed to load ${type} fields`, error)
          Vue.$toast.error(`[${code}] Failed to load ${type} fields: ${message}`)
          reject(error)
        })
    })
  }

  search (type, searchTerm, keyValuePairs) {
    const payload = {
      type,
      search_term: searchTerm,
      field_value_pairs: { ...keyValuePairs }
    }

    return new Promise((resolve, reject) => {
      axios.post('/api/search', payload, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const { hits } = response.data
          console.debug('advanced search response', hits.hits)
          resolve(hits.hits)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load search results', error)
          Vue.$toast.error(`[${code}] Failed to load search results: ${message}`)
          reject(error)
        })
    })
  }
}

export default new SearchService()
