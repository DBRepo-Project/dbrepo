import Vue from 'vue'
import axios from 'axios'
import { elasticPassword } from '../config'

class SearchService {
  search (query) {
    return new Promise((resolve, reject) => {
      axios.get(`/retrieve/_all/_search?q=${query}*&terminate_after=50`, { headers: { Accept: 'application/json' }, auth: { username: 'elastic', password: elasticPassword } })
        .then((response) => {
          const hits = response.data.hits.hits
          console.debug('response hits', hits)
          resolve(hits)
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
