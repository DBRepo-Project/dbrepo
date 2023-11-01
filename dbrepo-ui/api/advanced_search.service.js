import Vue from 'vue'
import axios from 'axios'

class AdvancedSearchService {
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

  search (searchData) {
    // transform values to what the search API expects
    const searchTerm = searchData.search_term
    delete searchData.search_term
    const payload = {
      search_term: searchTerm,
      fieldValuePairs: { ...searchData }
    }

    return new Promise((resolve, reject) => {
      axios.post('/api/search', payload, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const jsonResponse = response.data
          resolve(jsonResponse)
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

export default new AdvancedSearchService()
