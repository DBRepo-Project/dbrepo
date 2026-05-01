import * as tus from 'tus-js-client'
import {axiosErrorToApiError} from '@/utils'

export const useUploadService = (): any => {
  function legacy (data: File) {
    return new Promise<string>((resolve, reject) => {
      if (!tus.isSupported) {
        console.error('Your browser does not support uploads!')
        return
      }
      console.debug('===> Started', Date.now())
      const uploadClient: tus.Upload = new tus.Upload(data, {
        endpoint: 'https://s209.dl.hpc.tuwien.ac.at/files/',
        retryDelays: [0, 3000, 5000, 10000, 20000],
        onError(error) {
          console.error('Failed to upload:', error)
          reject(error)
        },
        onProgress(bytesUploaded, bytesTotal) {
          const percentage = ((bytesUploaded / bytesTotal) * 100).toFixed(2)
          console.debug(bytesUploaded, bytesTotal, percentage + '%')
        },
        onSuccess() {
          console.debug('===> Finished', Date.now())
          if (uploadClient.file) {
            const file: File = uploadClient.file as File
            console.info('Download %s from %s', file.name, uploadClient.url)
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

  function create (data: File) {
    const axios = useAxiosInstance();
    console.debug('upload file');
    return new Promise<string>((resolve, reject) => {
      const form = new FormData();
      form.append('file', data);
      axios.post<string>('/api/v1/upload', form, {
        headers: {
          'content-type': 'multipart/form-data'
        }
      })
        .then((response) => {
          const s3key: string = response.headers['x-s3-key']
          console.info(`Uploaded file: ${s3key}`);
          resolve(s3key);
        })
        .catch((error) => {
          console.error('Failed to upload file', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  return { legacy, create }
}
