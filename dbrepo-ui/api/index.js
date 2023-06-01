import axios from 'axios'
import { baseURL } from '../config'

const instance = axios.create({
  timeout: 10000,
  params: {},
  baseURL
})

export default instance
