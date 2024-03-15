export const useLicenseService = (): any => {
  async function findAll(): Promise<LicenseDto[]> {
    const axios = useAxiosInstance()
    try {
      console.debug('find licenses')
      const {data} = await axios.get<LicenseDto[]>('/api/database/license')
      console.info('Found license(s)')
      return data
    } catch (error) {
      console.error('Failed to find licenses', error)
      return []
    }
  }

  return {findAll}
}
