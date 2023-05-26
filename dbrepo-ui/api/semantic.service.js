import Vue from 'vue'
import api from '@/api'

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
          const { code, message } = error
          console.error('Failed to load ontologies', error)
          Vue.$toast.error(`[${code}] Failed to load ontologies: ${message}`)
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
          const { code, message } = error
          console.error('Failed to load concepts', error)
          Vue.$toast.error(`[${code}] Failed to load concepts: ${message}`)
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
          const { code, message } = error
          console.error('Failed to load units', error)
          Vue.$toast.error(`[${code}] Failed to load units: ${message}`)
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
          const { code, message } = error
          console.error('Failed to load ontology', error)
          Vue.$toast.error(`[${code}] Failed to load ontology: ${message}`)
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
          const { code, message } = error
          console.error('Failed to register ontology', error)
          Vue.$toast.error(`[${code}] Failed to register ontology: ${message}`)
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
          const { code, message } = error
          console.error('Failed to update ontology', error)
          Vue.$toast.error(`[${code}] Failed to update ontology: ${message}`)
          reject(error)
        })
    })
  }

  unregisterOntology (id) {
    return new Promise((resolve, reject) => {
      api.delete(`/api/semantic/ontology/${id}`, { headers: { Accept: 'application/json' } })
        .then(() => resolve())
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to delete ontology', error)
          Vue.$toast.error(`[${code}] Failed to delete ontology: ${message}`)
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
          const { code, message } = error
          console.error('Failed to load table column semantics', error)
          Vue.$toast.error(`[${code}] Failed to load table column semantics: ${message}`)
          reject(error)
        })
    })
  }
}

export default new SemanticService()
