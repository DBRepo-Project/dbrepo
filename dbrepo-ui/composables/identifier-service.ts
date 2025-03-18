import type {AxiosError, AxiosRequestConfig} from 'axios'
import {axiosErrorToApiError} from '@/utils'

export const useIdentifierService = (): any => {
  async function findOne(id: string, accept: string): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('find identifier with id', id)
    const config: AxiosRequestConfig = {
      headers: {
        Accept: accept
      }
    }
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.get<IdentifierDto>(`/api/identifier/${id}`, config)
        .then((response) => {
          console.info('Found identifier with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create identifier', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function create(data: IdentifierSaveDto): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('create identifier')
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.post<IdentifierDto>('/api/identifier', data)
        .then((response) => {
          console.info('Created identifier with id', response.data.id)
          resolve(response.data)
        })
        .catch((error: AxiosError) => {
          console.error('Failed to create identifier', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function save(data: IdentifierSaveDto): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('save identifier', data.id)
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.put<IdentifierDto>(`/api/identifier/${data.id}`, data)
        .then((response) => {
          console.info('Saved identifier with id', response.data.id)
          resolve(response.data)
        })
        .catch((error: AxiosError) => {
          console.error('Failed to save identifier', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function remove(id: string): Promise<void> {
    const axios = useAxiosInstance()
    console.debug('delete identifier', id)
    return new Promise<void>((resolve, reject) => {
      axios.delete<void>(`/api/identifier/${id}`)
        .then((response) => {
          console.info('Deleted identifier with id', id)
          resolve()
        })
        .catch((error: AxiosError) => {
          console.error('Failed to delete identifier', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function publish(id: string): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('publish identifier', id)
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.put<IdentifierDto>(`/api/identifier/${id}/publish`)
        .then((response) => {
          console.info('Published identifier with id', response.data.id)
          resolve(response.data)
        })
        .catch((error: AxiosError) => {
          console.error('Failed to publish identifier', error)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  async function suggest(uri: string): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('suggest metadata for identifier with uri', uri)
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.get<IdentifierDto>(`/api/identifier/retrieve?url=${uri}`)
        .then((response) => {
          console.info('Suggested metadata for identifier with uri', uri);
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to suggest metadata for identifier with uri', uri)
          reject(axiosErrorToApiError(error))
        })
    })
  }

  function identifierToCreators(identifier: IdentifierDto): string | null {
    if (!identifier) {
      return null
    }
    const creators = identifier.creators
    let str = ''
    for (let i = 0; i < creators.length; i++) {
      /* separator */
      if (creators.length > 1 && i === creators.length - 1) {
        str += ', & '
      } else if (i > 0 && creators.length !== 2) {
        str += ', '
      }
      /* name */
      str += creators[i].creator_name
    }
    return str
  }

  function identifierToIdentifierSave(data: IdentifierDto): IdentifierSaveDto {
    return {
      id: data.id,
      database_id: data.database_id,
      query_id: data.query_id,
      view_id: data.view_id,
      table_id: data.table_id,
      type: data.type,
      doi: data.doi,
      titles: data.titles.map((t) => {
        return {
          id: t.id,
          title: t.title,
          language: t.language,
          type: t.type
        }
      }),
      descriptions: data.descriptions.map((d) => {
        return {
          id: d.id,
          description: d.description,
          language: d.language,
          type: d.type
        }
      }),
      funders: data.funders.map((f) => {
        return {
          id: f.id,
          funder_name: f.funder_name,
          funder_identifier: f.funder_identifier,
          funder_identifier_type: f.funder_identifier_type,
          scheme_uri: f.scheme_uri,
          award_number: f.award_number,
          award_title: f.award_title
        }
      }),
      publisher: data.publisher,
      language: data.language,
      licenses: data.licenses,
      creators: data.creators.map((c) => {
        return {
          id: c.id,
          firstname: c.name_type === 'Personal' ? c.firstname : null,
          lastname: c.name_type === 'Personal' ? c.lastname : null,
          creator_name: c.creator_name,
          name_type: c.name_type,
          name_identifier: c.name_identifier,
          name_identifier_scheme: identifierToIdentifierScheme(c.name_identifier),
          affiliation: c.affiliation,
          affiliation_identifier: c.affiliation_identifier,
          affiliation_identifier_scheme: identifierToIdentifierScheme(c.affiliation_identifier)
        }
      }),
      publication_day: data.publication_day,
      publication_month: data.publication_month,
      publication_year: data.publication_year,
      related_identifiers: data.related_identifiers.map((r) => {
        return {
          id: r.id,
          value: r.value,
          type: r.type,
          relation: r.relation
        }
      })
    }
  }

  function identifierToIdentifierScheme(data: string): 'ROR' | 'ORCID' | 'GRID' | 'ISNI' | null {
    if (!data) {
      return null
    }
    if (data.includes('ror.org')) {
      return 'ROR'
    } else if (data.includes('orcid.org')) {
      return 'ORCID'
    } else if (data.includes('grid.ac')) {
      return 'GRID'
    } else if (data.includes('isni.org')) {
      return 'ISNI'
    }
    return null
  }

  function identifierToPreferFirstLicenseUri(data: IdentifierDto): string | null {
    if (!data || data.licenses.length === 0) {
      return null
    }
    return data.licenses[0].uri
  }

  function identifierPreferEnglishDescription(data: IdentifierDto): string | null {
    if (!data || !data.descriptions || data.descriptions.length === 0) {
      return null
    }
    const filtered = data.descriptions.filter(d => d.language && d.language === 'en')
    if (filtered.length === 0) {
      return data.descriptions[0]?.description
    }
    return filtered[0].description
  }

  function descriptionShort(description: string): string | null {
    if (!description) {
      return null
    }
    const targetLength = 280
    const lengthMax = 300
    if (description.length <= lengthMax) {
      return description
    }
    const extra = description.substring(targetLength, lengthMax)
    const idx = extra.indexOf(' ')
    return description.substring(0, targetLength + idx) + '...'
  }

  function identifierPreferEnglishTitle(data: IdentifierDto): string | null {
    if (!data || !data.titles || data.titles.length === 0) {
      return null
    }
    const filtered = data.titles.filter((d) => d.language && d.language === 'en')
    if (filtered.length === 0) {
      const title = data.titles[0]
      return title.title
    }
    return filtered[0].title
  }

  function identifierToResourceUrl(identifier: IdentifierDto): string | null {
    const config = useRuntimeConfig()
    switch (identifier.type) {
      case 1:
        return `${config.public.api.client}/api/database/${identifier.database_id}/subset/${identifier.subset_id}/data`
      case 2:
        return `${config.public.api.client}/api/database/${identifier.database_id}/table/${identifier.table_id}/data`
      case 3:
        return `${config.public.api.client}/api/database/${identifier.database_id}/view/${identifier.view_id}/data`
      default:
        return null
    }
  }

  function identifierToUrl(data: IdentifierDto): string | null {
    if (!data) {
      return null
    }
    const config = useRuntimeConfig()
    const val = data.doi ? data.doi : data.value
    if (val) {
      const regex: RegExp = /(10[.][0-9]{4,}[^\s"\/<>]*\/[^\s"<>]+)/g
      const matches: RegExpMatchArray | null = val.match(regex)
      if (matches && matches.length > 0) {
        if (config.public.doi.endpoint) {
          return `${config.public.doi.endpoint}/${matches[0]}`
        }
        return `https://doi.org/${matches[0]}`
      }
      if (val.startsWith('http')) {
        return val
      }
    }
    return `${config.public.api.client}/pid/${data.id}`
  }

  function identifierToDisplayName(data: IdentifierDto): string | null {
    if (!data) {
      return null
    }
    const config = useRuntimeConfig()
    const val = data.doi ? data.doi : data.value
    if (val) {
      const regex: RegExp = /(10[.][0-9]{4,}[^\s"\/<>]*\/[^\s"<>]+)/g
      const matches: RegExpMatchArray | null = val.match(regex)
      if (matches && matches.length > 0) {
        return matches[0]
      }
      return val
    }
    return `${config.public.api.client}/pid/${data.id}`
  }

  function identifierToDisplayAcronym(data: IdentifierDto): 'DOI' | 'URI' | null {
    if (!data) {
      return null
    }
    return data.doi ? 'DOI' : 'URI'
  }

  function creatorToCreatorJsonLd(creator: CreatorDto) {
    const jsonLd: any = {
      name: creator.creator_name
    }
    if (creator.name_type === 'Personal') {
      jsonLd['@type'] = 'Person'
      if (creator.name_identifier) {
        jsonLd.sameAs = creator.name_identifier
      }
      if (creator.firstname) {
        jsonLd.givenName = creator.firstname
      }
      if (creator.lastname) {
        jsonLd.familyName = creator.lastname
      }
    } else {
      jsonLd['@type'] = 'Organization'
      if (creator.affiliation_identifier) {
        jsonLd.sameAs = creator.affiliation_identifier
      }
    }
    return jsonLd
  }

  function identifiersToServerHead(identifiers: IdentifierBriefDto[]): any {
    if (!identifiers || !identifiers[0]) {
      return null
    }
    const identifier = identifiers[0]
    /* Google Rich Results */
    const json: any = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      url: identifierToUrl(identifier),
      citation: identifierToUrl(identifier),
      hasPart: [],
      identifier: identifiers.map(i => identifierToUrl(i)),
      creator: identifier.creators.map((c) => creatorToCreatorJsonLd(c)),
      temporalCoverage: identifier.publication_year
    }
    if (identifier.titles.length > 0) {
      json['name'] = identifierPreferEnglishTitle(identifier)
    }
    if (identifier.descriptions.length > 0) {
      json['description'] = identifierPreferEnglishDescription(identifier)
    }
    /* FAIR Signposting */
    const meta: any[] = []
    meta.push({rel: 'cite-as', href: identifierToUrl(identifier)})
    identifier.creators.forEach((c: CreatorDto) => {
      if (c.name_identifier) {
        meta.push({rel: 'author', href: c.name_identifier})
      }
    })
    meta.push({rel: 'describedby', type: 'application/x-bibtex', href: identifierToUrl(identifier)})
    meta.push({rel: 'describedby', type: 'application/vnd.datacite.datacite+json', href: identifierToUrl(identifier)})
    meta.push({
      rel: 'item',
      type: 'application/json',
      href: identifierToResourceUrl(identifier)
    })
    meta.push({
      rel: 'item',
      type: 'text/csv',
      href: identifierToResourceUrl(identifier)
    })
    return {
      script: [
        {
          type: 'application/ld+json',
          innerHTML: json
        }
      ],
      link: meta
    }
  }

  function identifiersToServerSeoMeta(identifiers: IdentifierBriefDto[]): any | null {
    if (!identifiers|| !identifiers[0]) {
      return null
    }
    const identifier = identifiers[0]
    const json: any = {
      ogTitle: identifierPreferEnglishTitle(identifier),
      ogDescription: identifierPreferEnglishDescription(identifier),
      description: identifierPreferEnglishDescription(identifier)
    }
    return json
  }

  return {
    findOne,
    create,
    save,
    remove,
    publish,
    suggest,
    identifierToCreators,
    identifierToIdentifierSave,
    identifierPreferEnglishDescription,
    descriptionShort,
    identifierPreferEnglishTitle,
    identifierToUrl,
    identifierToDisplayName,
    identifierToDisplayAcronym,
    identifiersToServerHead,
    identifiersToServerSeoMeta
  }
}
