import api, { displayError } from '@/api/index'

class SearchService {
  getFields (type) {
    return new Promise((resolve, reject) => {
      api.get(`/api/search/${type}/fields`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const json = response.data
          console.debug('fields result', json)
          resolve(json)
        })
        .catch((error) => {
          displayError('Failed to load fields', error)
          reject(error)
        })
    })
  }

  search (index, searchData) {
    // transform values to what the search API expects
    let localSearchData = Object.assign({}, searchData)
    const searchTerm = localSearchData.search_term
    delete localSearchData.search_term
    const t1 = localSearchData.t1
    delete localSearchData.t1
    const t2 = localSearchData.t2
    delete localSearchData.t2
    localSearchData = Object.fromEntries(Object.entries(localSearchData).filter(([_, v]) => v != null && v !== '')) // https://stackoverflow.com/questions/286141/remove-blank-attributes-from-an-object-in-javascript
    const payload = {
      t1,
      t2,
      search_term: searchTerm,
      field_value_pairs: { ...localSearchData }
    }
    return new Promise((resolve, reject) => {
      api.post(`/api/search${index ? `/${index}` : ''}`, payload, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const { hits } = response.data
          console.debug('advanced search response', hits.hits)
          resolve(hits.hits)
        })
        .catch((error) => {
          displayError('Failed to load search results', error)
          reject(error)
        })
    })
  }
}

export default new SearchService()
