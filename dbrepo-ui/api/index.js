import axios from 'axios'
import config from '../dbrepo.config.json'

const protocol = config.api.useSsl ? 'https' : 'http'
const baseUrl = `${protocol}://${config.api.endpoint}:${config.api.port}`

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL: baseUrl
})

export default instance
