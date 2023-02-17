const express = require('express')
const app = express()
const multer = require('multer')
const upload = multer({ dest: '/tmp' })

app.use(express.json())

const { buildQuery } = require('./query')

app.post('/upload', upload.single('file'), (req, res) => {
  const { file } = req
  return res.status(201)
    .json(file)
})

app.post('/query/build', (req, res) => {
  return res.json(buildQuery(req.body))
})

module.exports = app
