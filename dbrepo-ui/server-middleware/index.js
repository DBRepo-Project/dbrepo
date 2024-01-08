const express = require('express')
const { buildQuery } = require('./query')
const app = express()

app.use(express.json())

app.post('/query/build', (req, res) => {
  return res.json(buildQuery(req.body))
})

module.exports = app
