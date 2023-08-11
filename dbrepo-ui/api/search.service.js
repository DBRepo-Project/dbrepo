import Vue from 'vue'
import store from '@/store'
import axios from 'axios'

class SearchService {
  search (query) {
    return new Promise((resolve, reject) => {
      axios.get(`/retrieve/_all/_search?q=${query}*&terminate_after=50`, { headers: { Accept: 'application/json' }, auth: { username: store().state.searchUsername, password: store().state.searchPassword } })
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
