import * as tus from 'tus-js-client'
import {useCacheStore} from '@/stores/cache'
import {useUserStore} from '@/stores/user'

export const useUploadService = (): any => {

  function create (data: File) {
    const userStore = useUserStore()
    const config = useRuntimeConfig()
    const endpoint = config.public.upload.client
    return new Promise<string>((resolve, reject) => {
      if (!tus.isSupported) {
        console.error('Your browser does not support uploads!')
        return
      }
      const uploadClient: tus.Upload = new tus.Upload(data, {
        endpoint,
        headers: {
          'Authorization': `Bearer ${userStore.getToken}`
        },
        retryDelays: [0, 3000, 5000, 10000, 20000],
        onError (error) {
          console.error('Failed to upload:', error)
          reject(error)
        },
        onProgress (bytesUploaded, bytesTotal) {
          const percentage = ((bytesUploaded / bytesTotal) * 100).toFixed(2)
          console.debug(bytesUploaded, bytesTotal, percentage + '%')
          const cacheStore = useCacheStore()
          cacheStore.setUploadProgress(percentage)
        },
        onSuccess () {
          if (uploadClient.file) {
            const file: File = uploadClient.file as File
            console.info('Download %s from %s', file.name, uploadClient.url)
          }
          if (uploadClient.url) {
            const matches = uploadClient.url.match(/files\/([a-z0-9]+)/gi)
            if (!matches || matches.length !== 1) {
              console.error('Failed to match file name', matches)
              reject(new Error('Failed to match file name'))
            } else {
              const filename = matches[0].replace('files/', '')
              console.debug('Filename cropped as', filename)
              resolve(filename)
            }
          }
        }
      })
      uploadClient.findPreviousUploads().then(function (previousUploads) {
        /* Found previous uploads so we select the first one */
        if (previousUploads.length) {
          uploadClient.resumeFromPreviousUpload(previousUploads[0])
        }
        uploadClient.start()
      })
    })
  }

  return { create }
}
