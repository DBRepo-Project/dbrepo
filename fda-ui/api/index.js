import https from 'https'
import axios from 'axios'

const httpsAgent = new https.Agent({ rejectUnauthorized: false })

const instance = axios.create({
  timeout: 10000,
  params: {},
  httpsAgent
})

export default instance
