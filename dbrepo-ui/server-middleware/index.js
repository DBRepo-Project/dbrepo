const express = require('express')
const app = express()

app.use(express.json())

const { buildQuery } = require('./query')

app.post('/query/build', (req, res) => {
  return res.json(buildQuery(req.body))
})

module.exports = app
