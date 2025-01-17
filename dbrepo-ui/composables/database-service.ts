import {axiosErrorToApiError} from '@/utils'

export const useDatabaseService = (): any => {
  async function findAll(): Promise<DatabaseDto[]> {
    const axios = useAxiosInstance();
    console.debug('find databases');
    return new Promise<DatabaseDto[]>((resolve, reject) => {
      axios.get<DatabaseDto[]>('/api/database')
        .then((response) => {
          console.info(`Found ${response.data.length} database(s)`);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to find databases', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function refreshTablesMetadata(databaseId: number): Promise<DatabaseDto> {
    const axios = useAxiosInstance();
    console.debug('refresh database tables metadata');
    return new Promise<DatabaseDto>((resolve, reject) => {
      axios.put<DatabaseDto>('/api/database/' + databaseId + '/metadata/table', {})
        .then((response) => {
          console.info('Refreshed database tables metadata');
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to refresh database tables metadata', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function refreshViewsMetadata(databaseId: number): Promise<DatabaseDto> {
    const axios = useAxiosInstance();
    console.debug('refresh database views metadata');
    return new Promise<DatabaseDto>((resolve, reject) => {
      axios.put<DatabaseDto>('/api/database/' + databaseId + '/metadata/view', {})
        .then((response) => {
          console.info('Refreshed database views metadata');
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to refresh database views metadata', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function findCount(): Promise<number> {
    const axios = useAxiosInstance();
    console.debug('find databases count');
    return new Promise<number>((resolve, reject) => {
      axios.head<number>('/api/database')
        .then((response) => {
          const count: number = Number(response.headers['x-count'])
          console.info(`Found ${count} database(s)`);
          resolve(count);
        })
        .catch((error) => {
          console.error('Failed to find databases count', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function getServerTime(): Promise<Date> {
    const axios = useAxiosInstance();
    console.debug('find server time');
    return new Promise<Date>((resolve, reject) => {
      axios.head<Date>('/api/database')
        .then((response) => {
          const date: Date = new Date(response.headers['Date'])
          console.info(`Found ${date} server time`);
          resolve(date);
        })
        .catch((error) => {
          console.error('Failed to find server time', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function findOne(id: number, rawError: boolean = false): Promise<DatabaseDto | null> {
    const axios = useAxiosInstance();
    console.debug('find database with id', id);
    return new Promise((resolve, reject) => {
      axios.get<DatabaseDto>(`/api/database/${id}`)
        .then((response) => {
          console.info('Found database with id', id);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to find database', error);
          if (rawError) {
            reject(error)
          }
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function findPreviewImage(id: number): Promise<string> {
    const axios = useAxiosInstance();
    console.debug('find database preview image with id', id);
    return new Promise((resolve, reject) => {
      axios.get<string>(`/api/database/${id}/image`)
        .then((response) => {
          console.info('Found database preview image with id', id);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to find database preview image', error);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function updateVisibility(id: number, payload: DatabaseModifyVisibilityDto): Promise<DatabaseDto | null> {
    const axios = useAxiosInstance()
    console.debug('update database visibility for database with id', id);
    return new Promise((resolve, reject) => {
      axios.put<DatabaseDto>(`/api/database/${id}/visibility`, payload)
        .then((response) => {
          console.info('Updated database visibility for database with id', id);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to update database visibility for database with id', id);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function updateImage(id: number, payload: DatabaseModifyImageDto): Promise<DatabaseDto | null> {
    const axios = useAxiosInstance()
    console.debug('update database image for database with id', id);
    return new Promise((resolve, reject) => {
      axios.put<DatabaseDto>(`/api/database/${id}/image`, payload)
        .then((response) => {
          console.info('Updated database image for database with id', id);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to update database image for database with id', id);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function updateOwner(id: number, payload: DatabaseTransferDto): Promise<DatabaseDto | null> {
    const axios = useAxiosInstance()
    console.debug('update database owner for database with id', id);
    return new Promise((resolve, reject) => {
      axios.put<DatabaseDto>(`/api/database/${id}/owner`, payload)
        .then((response) => {
          console.info('Updated database owner for database with id', id);
          resolve(response.data);
        })
        .catch((error) => {
          console.error('Failed to update database owner for database with id', id);
          reject(axiosErrorToApiError(error));
        });
    });
  }

  async function create(payload: DatabaseCreateDto): Promise<DatabaseDto | void> {
    const axios = useAxiosInstance()
    console.debug('create databases')
    return new Promise((resolve, reject) => {
      axios.post<DatabaseDto>('/api/database', payload)
        .then((response) => {
          console.info('Created database with id', response.data.id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create databases', error)
          reject(axiosErrorToApiError(error));
        })
    })
  }

  function databaseToOwner(database: DatabaseDto) {
    if (!database) {
      return null
    }
    const userService = useUserService()
    return userService.userToFullName(database.owner)
  }

  function databaseToContact(database: DatabaseDto) {
    if (!database) {
      return null
    }
    const userService = useUserService()
    return userService.userToFullName(database.contact)
  }

  function databaseToJsonLd(database: DatabaseDto): Dataset {
    const jsonLd: Dataset = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      name: database.name,
      description: 'Relational database hosted by the database repository.',
      url: `/database/${database.id}/info`,
      isAccessibleForFree: database.is_public,
      creator: {
        '@type': 'Person',
        name: null,
        givenName: null,
        familyName: null,
        sameAs: null,
      }
    }
    jsonLd.creator.name = database.owner.name ? database.owner.name : database.owner.username
    if (database.owner.given_name) {
      jsonLd.creator.givenName = database.owner.given_name
    }
    if (database.owner.family_name) {
      jsonLd.creator.familyName = database.owner.family_name
    }
    if (database.owner.attributes.orcid) {
      jsonLd.creator.sameAs = database.owner.attributes.orcid
    }
    return jsonLd
  }

  function isOwner(database: DatabaseDto, user: UserDto): boolean {
    if (!database || !user) {
      return false
    }
    return database.owner.id === user.id
  }

  return {
    findAll,
    refreshTablesMetadata,
    refreshViewsMetadata,
    findOne,
    findPreviewImage,
    findCount,
    getServerTime,
    updateVisibility,
    updateImage,
    updateOwner,
    create,
    databaseToOwner,
    databaseToContact,
    databaseToJsonLd,
    isOwner
  }
}
