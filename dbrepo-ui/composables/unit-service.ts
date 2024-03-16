export const useUnitService = (): any => {
  async function findAll(): Promise<UnitDto[]> {
    const axios = useAxiosInstance()
    console.debug('find units')
    return new Promise<UnitDto[]>((resolve, reject) => {
      axios.get<UnitDto[]>('/api/semantic/unit')
        .then((response) => {
          console.info('Found unit(s)')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to find units', error)
          reject(error)
        })
    })
  }

  function mapUnits(units: UnitDto[]): UnitDto[] {
    return units.map((unit) => {
      unit.name = unit.name ? unit.name : unit.uri
      return unit
    })
  }

  return {findAll, mapUnits}
}
