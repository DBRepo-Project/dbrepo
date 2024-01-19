import api, { displayError } from '@/api'

class SemanticService {
  findAllOntologies () {
    return new Promise((resolve, reject) => {
      api.get('/api/semantic/ontology', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const ontologies = response.data
          console.debug('response ontologies', ontologies)
          resolve(ontologies)
        })
        .catch((error) => {
          displayError('Failed to load ontologies', error)
          reject(error)
        })
    })
  }

  findAllConcepts () {
    return new Promise((resolve, reject) => {
      api.get('/api/semantic/concept', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const concepts = response.data
          console.debug('response concepts', concepts)
          resolve(concepts)
        })
        .catch((error) => {
          displayError('Failed to load concepts', error)
          reject(error)
        })
    })
  }

  updateConcept (data) {
    return new Promise((resolve, reject) => {
      api.put('/api/semantic/concept', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const concept = response.data
          console.debug('response concept', concept)
          resolve(concept)
        })
        .catch((error) => {
          displayError('Failed to update concept', error)
          reject(error)
        })
    })
  }

  findAllUnits () {
    return new Promise((resolve, reject) => {
      api.get('/api/semantic/unit', { headers: { Accept: 'application/json' } })
        .then((response) => {
          const units = response.data
          console.debug('response units', units)
          resolve(units)
        })
        .catch((error) => {
          displayError('Failed to load units', error)
          reject(error)
        })
    })
  }

  updateUnit (data) {
    return new Promise((resolve, reject) => {
      api.put('/api/semantic/unit', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const unit = response.data
          console.debug('response unit', unit)
          resolve(unit)
        })
        .catch((error) => {
          displayError('Failed to update unit', error)
          reject(error)
        })
    })
  }

  findOntology (id) {
    return new Promise((resolve, reject) => {
      api.get(`/api/semantic/ontology/${id}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const ontology = response.data
          console.debug('response ontology', ontology)
          resolve(ontology)
        })
        .catch((error) => {
          displayError('Failed to find ontology', error)
          reject(error)
        })
    })
  }

  registerOntology (data) {
    return new Promise((resolve, reject) => {
      api.post('/api/semantic/ontology', data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const ontology = response.data
          console.debug('response ontology', ontology)
          resolve(ontology)
        })
        .catch((error) => {
          displayError('Failed to register ontology', error)
          reject(error)
        })
    })
  }

  updateOntology (id, data) {
    return new Promise((resolve, reject) => {
      api.put(`/api/semantic/ontology/${id}`, data, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const ontology = response.data
          console.debug('response ontology', ontology)
          resolve(ontology)
        })
        .catch((error) => {
          displayError('Failed to update ontology', error)
          reject(error)
        })
    })
  }

  unregisterOntology (id) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/semantic/ontology/${id}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          displayError('Failed to unregister ontology', error)
          reject(error)
        })
    })
  }

  suggestTableColumn (id, tableId, columnId) {
    return new Promise((resolve, reject) => {
      api.get(`/api/semantic/database/${id}/table/${tableId}/column/${columnId}`, { headers: { Accept: 'application/json' } })
        .then((response) => {
          const semantics = response.data
          console.debug('response semantics', semantics)
          resolve(semantics)
        })
        .catch((error) => {
          displayError('Failed to suggest table column semantic', error)
          reject(error)
        })
    })
  }
}

export default new SemanticService()
