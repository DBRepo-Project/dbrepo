import Vue from 'vue'
import axios from 'axios'
import { brokerUsername, brokerPassword } from '../config'

class BrokerService {
  findConsumers () {
    return new Promise((resolve, reject) => {
      const basic = btoa(`${brokerUsername}:${brokerPassword}`)
      axios.get('/api/broker/consumers/%2F', { headers: { Authorization: 'Basic ' + basic } })
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
}

export default new BrokerService()
