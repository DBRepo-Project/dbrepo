import store from '@/store'
const baseUrl = `${location.protocol}//${location.host}`

class IdentifierMapper {
  identifierToCreators (identifier) {
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

  identifierToIdentifierSave (data) {
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
          funder_identifier_type: f.funder_identifier_scheme,
          scheme_uri: f.scheme_uri,
          award_number: f.award_number,
          award_title: f.award_title
        }
      }),
      visibility: data.visibility,
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
          affiliation_identifier_scheme: this.identifierToIdentifierScheme(c.affiliation_identifier)
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

  identifierToIdentifierScheme (data) {
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

  identifierToPreferFirstLicenseUri (identifier) {
    if (!identifier || !identifier.licenses || identifier.licenses.length === 0) {
      return null
    }
    return identifier.licenses[0].uri
  }

  identifierPreferEnglishDescription (identifier) {
    if (!identifier || !identifier.descriptions || identifier.descriptions.length === 0) {
      return null
    }
    const filtered = identifier.descriptions.filter(d => d.language && d.language === 'en')
    if (filtered.length === 0) {
      return identifier.descriptions[0].description
    }
    return filtered[0].description
  }

  descriptionShort (description) {
    const targetLength = 280
    const lengthMax = 300
    if (!description) {
      return null
    }
    if (description.length <= lengthMax) {
      return description
    }
    const extra = description.substring(targetLength, lengthMax)
    const idx = extra.indexOf(' ')
    return description.substring(0, targetLength + idx) + '...'
  }

  identifierPreferEnglishTitle (identifier) {
    if (!identifier || !identifier.titles || identifier.titles.length === 0) {
      return null
    }
    const filtered = identifier.titles.filter(d => d.language && d.language === 'en')
    if (filtered.length === 0) {
      return identifier.titles[0].title
    }
    return filtered[0].title
  }

  identifierToUrl (identifier) {
    if (!identifier) {
      return null
    }
    if (identifier.doi !== null) {
      if (identifier.doi.startsWith('http')) {
        return identifier.doi
      }
      return `${store().state.doiUrl}/${identifier.doi}`
    }
    return `${baseUrl}/pid/${identifier.id}`
  }

  identifierToDisplayName (identifier) {
    if (!identifier) {
      return null
    }
    if (identifier.doi !== null) {
      if (identifier.doi.startsWith('http')) {
        return identifier.replaceAll('https?://doi.org/', '')
      }
      return identifier.doi
    }
    return `${baseUrl}/pid/${identifier.id}`
  }

  identifierToDisplayAcronym (identifier) {
    if (!identifier) {
      return null
    }
    return identifier.doi !== null ? 'DOI' : 'URI'
  }

  creatorToCreatorJsonLd (creator) {
    const jsonLd = {
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

  identifierToHasPartJsonLd (identifier) {
    return {
      '@type': 'Dataset',
      name: this.identifierPreferEnglishTitle(identifier),
      description: this.identifierPreferEnglishDescription(identifier),
      identifier: this.identifierToUrl(identifier),
      citation: this.identifierToUrl(identifier),
      temporalCoverage: identifier.publication_year,
      version: identifier.created
    }
  }

  identifiersToJsonLd (database) {
    const identifier = database.identifiers[0]
    const partIdentifiers = []
    if (database.subsets.length > 0) {
      database.subsets.forEach((s) => { if (s.identifiers.length > 0) { s.identifiers.forEach(i => partIdentifiers.push(i)) } })
    }
    if (database.tables.length > 0) {
      database.tables.forEach((t) => { if (t.identifiers.length > 0) { t.identifiers.forEach(i => partIdentifiers.push(i)) } })
    }
    if (database.views.length > 0) {
      database.views.forEach((v) => { if (v.identifiers.length > 0) { v.identifiers.forEach(i => partIdentifiers.push(i)) } })
    }
    return {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      name: this.identifierPreferEnglishTitle(identifier),
      description: this.identifierPreferEnglishDescription(identifier),
      url: `${baseUrl}/database/${identifier.database_id}/info`,
      identifier: database.identifiers.map(i => this.identifierToUrl(i)),
      license: this.identifierToPreferFirstLicenseUri(identifier),
      isAccessibleForFree: database.is_public,
      creator: identifier.creators.map(c => this.creatorToCreatorJsonLd(c)),
      citation: this.identifierToUrl(identifier),
      hasPart: partIdentifiers.map(i => this.identifierToHasPartJsonLd(i)),
      temporalCoverage: identifier.publication_year,
      version: identifier.created
    }
  }
}

export default new IdentifierMapper()
