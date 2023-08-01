import Vue from 'vue'
import { uploadEndpoint, uploadPath } from '../config'
const tus = require('tus-js-client')

class UploadService {
  upload (file) {
    return new Promise((resolve, reject) => {
      const upload = new tus.Upload(file, {
        endpoint: uploadEndpoint,
        retryDelays: [0, 3000, 5000, 10000, 20000],
        metadata: {
          filename: file.name,
          filetype: file.type
        },
        onError (error) {
          console.error('Failed because: ' + error)
        },
        onProgress (bytesUploaded, bytesTotal) {
          const percentage = ((bytesUploaded / bytesTotal) * 100).toFixed(2)
          console.debug(bytesUploaded, bytesTotal, percentage + '%')
        },
        onSuccess () {
          console.info('Download %s from %s', upload.file.name, upload.url)
          Vue.$toast.success('Successfully uploaded file')
          upload.path = (uploadPath || '') + upload.url.replace(uploadEndpoint, '')
          resolve(upload)
        }
      })
      upload.findPreviousUploads().then(function (previousUploads) {
        /* Found previous uploads so we select the first one */
        if (previousUploads.length) {
          upload.resumeFromPreviousUpload(previousUploads[0])
        }
        upload.start()
      })
    })
  }
}

export default new UploadService()
