import {axiosErrorToApiError} from '@/utils'

export const useUserService = (): any => {
  async function findAll(): Promise<UserDto[]> {
    const axios = useAxiosInstance()
    console.debug('find users')
    return new Promise<UserDto[]>((resolve, reject) => {
      axios.get<UserDto[]>('/api/user')
        .then((response) => {
          console.info('Found user(s)');
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to find users', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function findOne(id: string): Promise<UserDto> {
    const axios = useAxiosInstance()
    console.debug('find user with id', id)
    return new Promise<UserDto>((resolve, reject) => {
      axios.get<UserDto>(`/api/user/${id}`)
        .then((response) => {
          console.info('Found user with id', id);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to find user', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function update(id: string, data: UserUpdateDto): Promise<UserDto> {
    const axios = useAxiosInstance()
    console.debug('update user with id', id)
    return new Promise<UserDto>((resolve, reject) => {
      axios.put<UserDto>(`/api/user/${id}`, data)
        .then((response) => {
          console.info('Updated user with id', id)
          resolve(response.data)
        }).catch((error) => {
        console.error('Failed to update user', error)
        reject(axiosErrorToApiError(error))
      })
    })
  }

  async function create(data: SignupRequestDto): Promise<UserDto> {
    const axios = useAxiosInstance()
    console.debug('create user')
    return new Promise<UserDto>((resolve, reject) => {
      axios.post<UserDto>('/api/user', data)
        .then((response) => {
          console.info('Create user')
          resolve(response.data)
        }).catch((error) => {
        console.error('Failed to create user', error)
        reject(axiosErrorToApiError(error))
      })
    })
  }

  async function updatePassword(id: string, data: UserPasswordDto): Promise<UserDto> {
    const axios = useAxiosInstance()
    console.debug('update user password for user with id', id)
    return new Promise<UserDto>((resolve, reject) => {
      axios.put<UserDto>(`/api/user/${id}/password`, data)
        .then((response) => {
          console.info('Update user password for user with id', id)
          resolve(response.data)
        }).catch((error) => {
        console.error('Failed to update user password', error)
        reject(axiosErrorToApiError(error))
      })
    })
  }

  function nameIdentifierToNameIdentifierScheme(nameIdentifier: string) {
    if (nameIdentifier.includes('orcid.org')) {
      return 'ORCID'
    } else if (nameIdentifier.includes('ror.org')) {
      return 'ROR'
    } else if (nameIdentifier.includes('isni.org')) {
      return 'ISNI'
    } else if (nameIdentifier.includes('grid.ac')) {
      return 'GRID'
    }
    return null
  }

  function userToFullName(user: UserDto) {
    if (!user) {
      return null
    }
    if (!('given_name' in user) || !('family_name' in user) || user.given_name === null || user.family_name === null) {
      return user.username
    }
    return user.given_name + ' ' + user.family_name
  }

  function hasReadAccess(access: DatabaseAccessDto): boolean {
    if (!access) {
      return false
    }
    return access.type === 'read' || access.type === 'write_own' || access.type === 'write_all'
  }

  function hasWriteAccess(table: TableDto, access: DatabaseAccessDto, user: UserDto): boolean {
    if (!table || !access || !user) {
      return false
    }
    if (access.type === 'write_all') {
      return true
    }
    return access.type === 'write_own' && table.owner.id === user.uid
  }

  return {
    findAll,
    findOne,
    update,
    create,
    updatePassword,
    nameIdentifierToNameIdentifierScheme,
    userToFullName,
    hasReadAccess,
    hasWriteAccess
  }
}
