import {axiosErrorToApiError} from '@/utils'

export const useUploadService = (): any => {
  function create (data: File) {
    const axios = useAxiosInstance();
    console.debug('upload file');
    return new Promise<UploadResponseDto>((resolve, reject) => {
      const form = new FormData();
      form.append('file', data);
      axios.post<UploadResponseDto>('/api/upload', form, {
        headers: {
          'content-type': 'multipart/form-data'
        }
      })
        .then((response) => {
          console.info(`Uploaded file: ${response.data.s3_key}`);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to upload file', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  return { create }
}
