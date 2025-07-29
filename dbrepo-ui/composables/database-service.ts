import {axiosErrorToApiError} from '@/utils'

export const useDatabaseService = (): any => {
  async function determineSchema (databaseId: string, s3key: string): Promise<SchemaAnalysisResultDto> {
    const axios = useAxiosInstance()
    console.debug(`suggest data types for dataset: ${s3key}`)
    return new Promise<SchemaAnalysisResultDto>((resolve, reject) => {
      axios.get<SchemaAnalysisResultDto>(`/api/v1/database/${databaseId}/analyse/schema/${s3key}`)
        .then((response) => {
          console.info('Determined schema for dataset')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to determine schema for dataset', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function findAll(limit: number | null = null): Promise<DatabaseDto[]> {
    const axios = useAxiosInstance();
    console.debug('find databases');
    return new Promise<DatabaseDto[]>((resolve, reject) => {
      axios.get<DatabaseDto[]>(`/api/v1/database?limit=${limit ? limit : ''}`)
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

  async function refreshTablesMetadata(databaseId: string): Promise<DatabaseDto> {
    const axios = useAxiosInstance();
    console.debug('refresh database tables metadata');
    return new Promise<DatabaseDto>((resolve, reject) => {
      axios.put<DatabaseDto>('/api/v1/database/' + databaseId + '/metadata/table', {})
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

  async function refreshViewsMetadata(databaseId: string): Promise<DatabaseDto> {
    const axios = useAxiosInstance();
    console.debug('refresh database views metadata');
    return new Promise<DatabaseDto>((resolve, reject) => {
      axios.put<DatabaseDto>('/api/v1/database/' + databaseId + '/metadata/view', {})
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
      axios.head<number>('/api/v1/database')
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

  async function findOne(id: string, rawError: boolean = false): Promise<DatabaseDto> {
    const axios = useAxiosInstance();
    console.debug('find database with id', id);
    return new Promise((resolve, reject) => {
      axios.get<DatabaseDto>(`/api/v1/database/${id}`)
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

  async function findPreviewImage(id: string): Promise<string> {
    const axios = useAxiosInstance();
    console.debug('find database preview image with id', id);
    return new Promise((resolve, reject) => {
      axios.get<string>(`/api/v1/database/${id}/image`)
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

  async function updateVisibility(id: string, payload: DatabaseModifyVisibilityDto): Promise<DatabaseDto> {
    const axios = useAxiosInstance()
    console.debug('update database visibility for database with id', id);
    return new Promise((resolve, reject) => {
      axios.put<DatabaseDto>(`/api/v1/database/${id}/visibility`, payload)
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

  async function updateImage(id: string, payload: DatabaseModifyImageDto): Promise<DatabaseDto> {
    const axios = useAxiosInstance()
    console.debug('update database image for database with id', id);
    return new Promise((resolve, reject) => {
      axios.put<DatabaseDto>(`/api/v1/database/${id}/image`, payload)
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

  async function updateOwner(id: string, payload: DatabaseTransferDto): Promise<DatabaseDto> {
    const axios = useAxiosInstance()
    console.debug('update database owner for database with id', id);
    return new Promise((resolve, reject) => {
      axios.put<DatabaseDto>(`/api/v1/database/${id}/owner`, payload)
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
      axios.post<DatabaseDto>('/api/v1/database', payload)
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
    return database.owner.id === user.uid
  }

  return {
    determineSchema,
    findAll,
    findCount,
    refreshTablesMetadata,
    refreshViewsMetadata,
    findOne,
    updateVisibility,
    updateImage,
    updateOwner,
    create,
    databaseToOwner,
    databaseToContact,
    isOwner
  }
}
