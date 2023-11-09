const express = require('express')
const multer = require('multer')
const Minio = require('minio')
const { s3storageHostname, s3storagePort, forceSsl, s3accessKeyId, s3secretAccessKey } = require('../config')
const { buildQuery } = require('./query')
const app = express()

app.use(express.json())

const minioClient = new Minio.Client({
  endPoint: s3storageHostname,
  port: s3storagePort,
  useSSL: forceSsl,
  accessKeyId: s3accessKeyId,
  secretAccessKey: s3secretAccessKey
})

app.post('/query/build', (req, res) => {
  return res.json(buildQuery(req.body))
})

app.post('/upload', multer().single('file'), function (req, res) {
  const { file } = req
  try {
    minioClient.putObject('dbrepo-upload', file.originalname, file.buffer, function (err, etag) {
      if (err) {
        console.error('Failed to upload file', err)
        return res.sendStatus(403)
      }
      console.debug('Successfully uploaded file', etag)
      file.etag = etag
      return res.status(201).json(file)
    })
  } catch (err) {
    console.error('Failed to upload file', err)
    return res.sendStatus(403)
  }
})

module.exports = app
