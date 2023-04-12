import axios from 'axios'
import { api as endpoint } from '@/config'

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL: endpoint,
  headers: {
    'Access-Control-Allow-Origin': '*'
  }
})

export default instance
