import type {AxiosRequestConfig} from 'axios'

export const useIdentifierService = (): any => {
  async function findOne(id: number, accept: string | null): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('find identifier with id', id)
    const config: AxiosRequestConfig = {
      headers: {
        Accept: accept
      }
    }
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.get<IdentifierDto>(`/api/pid/${id}`, config)
        .then((response) => {
          console.info('Found identifier with id', id)
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create identifier', error)
          reject(error)
        })
    })
  }

  async function create(data: IdentifierSaveDto): Promise<IdentifierDto> {
    const axios = useAxiosInstance()
    console.debug('create identifier')
    return new Promise<IdentifierDto>((resolve, reject) => {
      axios.post<IdentifierDto>('/api/identifier', data)
        .then((response) => {
          console.info('Created identifier')
          resolve(response.data)
        })
        .catch((error) => {
          console.error('Failed to create identifier', error)
          reject(error)
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
          reject(error)
        })
    })
  }

  function identifierToCreators(identifier: IdentifierDto) {
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
      database_id: data.database_id,
      query_id: data.query_id,
      view_id: data.view_id,
      table_id: data.table_id,
      type: data.type,
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
          name_identifier_scheme: c.name_identifier_scheme,
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
    if (!data) {
      return null
    }
    const filtered = data.descriptions.filter(d => d.language && d.language === 'en')
    if (filtered.length === 0) {
      return data.descriptions[0].description
    }
    return filtered[0].description
  }

  function descriptionShort(description: string): string {
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
    if (!data) {
      return null
    }
    const filtered = data.titles.filter(d => d.language && d.language === 'en')
    if (filtered.length === 0) {
      return data.titles[0].title
    }
    return filtered[0].title
  }

  function identifierToUrl(data: IdentifierDto): string | null {
    if (!data) {
      return null
    }
    const config = useRuntimeConfig()
    if (data.doi !== null) {
      if (data.doi.startsWith('http')) {
        return data.doi
      }
      return `${config.public.doi}/${data.doi}`
    }
    return `${config.public.api.client}/pid/${data.id}`
  }

  function identifierToDisplayName(data: IdentifierDto): string | null {
    if (!data) {
      return null
    }
    const config = useRuntimeConfig()
    if (data.doi !== null) {
      if (data.doi.startsWith('http')) {
        return data.doi.replaceAll('https?://doi.org/', '')
      }
      return data.doi
    }
    return `${config.public.api.client}/pid/${data.id}`
  }

  function identifierToDisplayAcronym(data: IdentifierDto): 'DOI' | 'URI' | null {
    if (!data) {
      return null
    }
    return data.doi !== null ? 'DOI' : 'URI'
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

  function identifierToHasPartJsonLd(identifier: IdentifierDto) {
    return {
      '@type': 'Dataset',
      name: identifierPreferEnglishTitle(identifier),
      description: identifierPreferEnglishDescription(identifier),
      identifier: identifierToUrl(identifier),
      citation: identifierToUrl(identifier),
      temporalCoverage: identifier.publication_year,
      version: identifier.created
    }
  }

  function databaseToServerHead(database: DatabaseDto) {
    const config = useRuntimeConfig()
    /* Google Rich Results */
    const json: any = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      url: `${config.public.api.client}/database/${database.id}/info`,
      citation: `${config.public.api.client}/database/${database.id}/info`,
      hasPart: [],
      version: database.created
    }
    /* FAIR Signposting */
    const meta: any [] = []
    if (database.identifiers.length > 0) {
      const identifier = database.identifiers[0]
      const partIdentifiers: IdentifierDto[] = []
      if (database.subsets.length > 0) {
        database.subsets.forEach((s) => {
          partIdentifiers.push(s)
        })
      }
      if (database.tables.length > 0) {
        database.tables.forEach((t) => {
          if (t.identifiers.length > 0) {
            t.identifiers.forEach(i => partIdentifiers.push(i))
          }
        })
      }
      if (database.views.length > 0) {
        database.views.forEach((v) => {
          if (v.identifiers.length > 0) {
            v.identifiers.forEach(i => partIdentifiers.push(i))
          }
        })
      }
      json['name'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['identifier'] = database.identifiers.map(i => identifierToUrl(i))
      json['license'] = identifierToPreferFirstLicenseUri(identifier)
      json['creator'] = identifier.creators.map(c => creatorToCreatorJsonLd(c))
      json['citation'] = identifierToUrl(identifier)
      json['hasPart'] = partIdentifiers.map(i => identifierToHasPartJsonLd(i))
      json['temporalCoverage'] = identifier.publication_year
      meta.push({rel: 'cite-as', href: identifierToUrl(identifier)})
      identifier.creators.forEach((c: CreatorDto) => {
        if (c.name_identifier) {
          meta.push({rel: 'author', href: c.name_identifier})
        }
      })
      meta.push({rel: 'describedby', type: 'application/x-bibtex', href: identifierToUrl(identifier)})
      meta.push({rel: 'describedby', type: 'application/vnd.datacite.datacite+json', href: identifierToUrl(identifier)})
      identifier.licenses.forEach((l: LicenseDto) => meta.push({rel: 'license', href: l.uri}))
    }
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

  function subsetToServerHead(subset: QueryDto) {
    const config = useRuntimeConfig()
    /* Google Rich Results */
    const json: any = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      description: subset.query,
      url: `${config.public.api.client}/database/${subset.database_id}/info`,
      citation: `${config.public.api.client}/database/${subset.database_id}/info`,
      hasPart: [],
      version: subset.created
    }
    /* FAIR Signposting */
    const meta: any[] = []
    if (subset.identifiers.length > 0) {
      const identifier = subset.identifiers[0]
      json['name'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['identifier'] = subset.identifiers.map(i => identifierToUrl(i))
      json['license'] = identifierToPreferFirstLicenseUri(identifier)
      json['creator'] = identifier.creators.map(c => creatorToCreatorJsonLd(c))
      json['citation'] = identifierToUrl(identifier)
      json['temporalCoverage'] = identifier.publication_year
      meta.push({rel: 'cite-as', href: identifierToUrl(identifier)})
      identifier.creators.forEach((c: CreatorDto) => {
        if (c.name_identifier) {
          meta.push({rel: 'author', href: c.name_identifier})
        }
      })
      meta.push({rel: 'describedby', type: 'application/x-bibtex', href: identifierToUrl(identifier)})
      meta.push({rel: 'describedby', type: 'application/vnd.datacite.datacite+json', href: identifierToUrl(identifier)})
      identifier.licenses.forEach((l: LicenseDto) => meta.push({rel: 'license', href: l.uri}))
      meta.push({
        rel: 'item',
        type: 'application/json',
        href: `${config.public.api.client}/api/database/${subset.database_id}/query/${subset.id}/data`
      })
      meta.push({
        rel: 'item',
        type: 'text/csv',
        href: `${config.public.api.client}/api/database/${subset.database_id}/query/${subset.id}/data`
      })
    }
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

  function tableToServerHead(table: TableDto) {
    const config = useRuntimeConfig()
    /* Google Rich Results */
    const json: any = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      description: table.description,
      url: `${config.public.api.client}/database/${table.tdbid}/table/${table.id}/info`,
      citation: `${config.public.api.client}/database/${table.tdbid}/table/${table.id}/info`,
      hasPart: [],
      version: table.created
    }
    /* FAIR Signposting */
    const meta: any[] = []
    if (table.identifiers.length > 0) {
      const identifier: IdentifierDto = table.identifiers[0]
      json['name'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['identifier'] = table.identifiers.map((i: IdentifierDto) => identifierToUrl(i))
      json['license'] = identifierToPreferFirstLicenseUri(identifier)
      json['creator'] = identifier.creators.map((c: CreatorDto) => creatorToCreatorJsonLd(c))
      json['citation'] = identifierToUrl(identifier)
      json['temporalCoverage'] = identifier.publication_year
      meta.push({rel: 'cite-as', href: identifierToUrl(identifier)})
      identifier.creators.forEach((c: CreatorDto): void => {
        if (c.name_identifier) {
          meta.push({rel: 'author', href: c.name_identifier})
        }
      })
      meta.push({rel: 'describedby', type: 'application/x-bibtex', href: identifierToUrl(identifier)})
      meta.push({rel: 'describedby', type: 'application/vnd.datacite.datacite+json', href: identifierToUrl(identifier)})
      identifier.licenses.forEach((l: LicenseDto) => meta.push({rel: 'license', href: l.uri}))
      meta.push({
        rel: 'item',
        type: 'application/json',
        href: `${config.public.api.client}/api/database/${table.tdbid}/table/${table.id}/data`
      })
      meta.push({
        rel: 'item',
        type: 'text/csv',
        href: `${config.public.api.client}/api/database/${table.tdbid}/table/${table.id}/data`
      })
    }
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

  function viewToServerHead(view: ViewDto) {
    const config = useRuntimeConfig()
    /* Google Rich Results */
    const json: any = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      description: view.query,
      url: `${config.public.api.client}/database/${view.database_id}/table/${view.id}/info`,
      citation: `${config.public.api.client}/database/${view.database_id}/table/${view.id}/info`,
      hasPart: [],
      version: view.created
    }
    /* FAIR Signposting */
    const meta: any[] = []
    if (view.identifiers.length > 0) {
      const identifier = view.identifiers[0]
      json['name'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['identifier'] = view.identifiers.map(i => identifierToUrl(i))
      json['license'] = identifierToPreferFirstLicenseUri(identifier)
      json['creator'] = identifier.creators.map(c => creatorToCreatorJsonLd(c))
      json['citation'] = identifierToUrl(identifier)
      json['temporalCoverage'] = identifier.publication_year
      meta.push({rel: 'cite-as', href: identifierToUrl(identifier)})
      identifier.creators.forEach((c: CreatorDto) => {
        if (c.name_identifier) {
          meta.push({rel: 'author', href: c.name_identifier})
        }
      })
      meta.push({rel: 'describedby', type: 'application/x-bibtex', href: identifierToUrl(identifier)})
      meta.push({rel: 'describedby', type: 'application/vnd.datacite.datacite+json', href: identifierToUrl(identifier)})
      identifier.licenses.forEach((l: LicenseDto) => meta.push({rel: 'license', href: l.uri}))
      meta.push({
        rel: 'item',
        type: 'application/json',
        href: `${config.public.api.client}/api/database/${view.database_id}/view/${view.id}/data`
      })
      meta.push({
        rel: 'item',
        type: 'text/csv',
        href: `${config.public.api.client}/api/database/${view.database_id}/view/${view.id}/data`
      })
    }
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

  function databaseToServerSeoMeta(database: DatabaseDto) {
    const json: any = {
      ogTitle: database.name
    }
    if (database.identifiers.length > 0) {
      const identifier = database.identifiers[0]
      json['ogTitle'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['ogDescription'] = identifierPreferEnglishDescription(identifier)
    }
    return json
  }

  function subsetToServerSeoMeta(subset: QueryDto) {
    const json: any = {
      description: subset.query
    }
    if (subset.identifiers.length > 0) {
      const identifier = subset.identifiers[0]
      json['ogTitle'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['ogDescription'] = identifierPreferEnglishDescription(identifier)
    }
    return json
  }

  function tableToServerSeoMeta(table: TableDto) {
    const json: any = {
      ogTitle: table.name,
      description: table.description
    }
    if (table.identifiers.length > 0) {
      const identifier = table.identifiers[0]
      json['ogTitle'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['ogDescription'] = identifierPreferEnglishDescription(identifier)
    }
    return json
  }

  function viewToServerSeoMeta(view: ViewDto) {
    const json: any = {
      ogTitle: view.name,
      description: view.query
    }
    if (view.identifiers.length > 0) {
      const identifier = view.identifiers[0]
      json['ogTitle'] = identifierPreferEnglishTitle(identifier)
      json['description'] = identifierPreferEnglishDescription(identifier)
      json['ogDescription'] = identifierPreferEnglishDescription(identifier)
    }
    return json
  }

  return {
    findOne,
    create,
    suggest,
    identifierToCreators,
    identifierToIdentifierSave,
    identifierToIdentifierScheme,
    identifierToPreferFirstLicenseUri,
    identifierPreferEnglishDescription,
    descriptionShort,
    identifierPreferEnglishTitle,
    identifierToUrl,
    identifierToDisplayName,
    identifierToDisplayAcronym,
    databaseToServerHead,
    subsetToServerHead,
    tableToServerHead,
    viewToServerHead,
    databaseToServerSeoMeta,
    subsetToServerSeoMeta,
    tableToServerSeoMeta,
    viewToServerSeoMeta,
  }
}
