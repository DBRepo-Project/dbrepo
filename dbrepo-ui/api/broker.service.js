import Vue from 'vue'
import store from '@/store'
import axios from 'axios'

class BrokerService {
  findConsumers () {
    return new Promise((resolve, reject) => {
      const basic = btoa(`${store().state.brokerUsername}:${store().state.brokerPassword}`)
      axios.get('/api/broker/consumers/dbrepo', { headers: { Authorization: 'Basic ' + basic } })
        .then((response) => {
          const consumers = response.data
          console.debug('response consumers', consumers)
          resolve(consumers)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load consumers', error)
          Vue.$toast.error(`[${code}] Failed to load consumers: ${message}`)
          reject(error)
        })
    })
  }

  findExchange (name) {
    return new Promise((resolve, reject) => {
      const basic = btoa(`${store().state.brokerUsername}:${store().state.brokerPassword}`)
      axios.get(`/api/broker/exchanges/dbrepo/${name}`, { headers: { Authorization: 'Basic ' + basic } })
        .then((response) => {
          const exchange = response.data
          console.debug('response exchange', exchange)
          resolve(exchange)
        })
        .catch((error) => {
          const { code, message } = error
          console.error('Failed to load exchange', error)
          Vue.$toast.error(`[${code}] Failed to load exchange: ${message}`)
          reject(error)
        })
    })
  }
}

export default new BrokerService()
