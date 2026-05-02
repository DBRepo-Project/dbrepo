import {axiosErrorToApiError} from '@/utils'

export const useUploadService = (): any => {
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
