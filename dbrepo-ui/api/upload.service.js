import Vue from 'vue'
const tus = require('tus-js-client')

class UploadService {
  upload (url, file) {
    return new Promise((resolve, reject) => {
      const endpoint = `${url}/api/upload/files`
      console.debug('upload endpoint', endpoint)
      if (!tus.isSupported) {
        console.error('Your browser does not support uploads!')
        Vue.$toast.error('Your browser does not support uploads!')
        return
      }
      const upload = new tus.Upload(file, {
        endpoint,
        retryDelays: [0, 3000, 5000, 10000, 20000],
        metadata: {
          filename: file.name,
          filetype: file.type
        },
        onError (error) {
          console.error('Failed to upload:', error)
          reject(error)
        },
        onProgress (bytesUploaded, bytesTotal) {
          const percentage = ((bytesUploaded / bytesTotal) * 100).toFixed(2)
          console.debug(bytesUploaded, bytesTotal, percentage + '%')
        },
        onSuccess () {
          console.info('Download %s from %s', upload.file.name, upload.url)
          Vue.$toast.success('Successfully uploaded file')
          const matches = upload.url.match(/files\/([a-z0-9]+)/gi)
          if (matches.length !== 1) {
            console.error('Failed to match file name', matches)
            reject(new Error('Failed to match file name'))
          }
          upload.s3key = matches[0].replace('files/', '')
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
