import https from 'https'
import axios from 'axios'
import api from '@/config'

const httpsAgent = new https.Agent({ rejectUnauthorized: false })

const instance = axios.create({
  baseURL: api,
  timeout: 10000,
  params: {},
  httpsAgent
})

export default instance
