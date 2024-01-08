import store from '@/store'

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
    return `/pid/${identifier.id}`
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
    return `/pid/${identifier.id}`
  }

  identifierToDisplayAcronym (identifier) {
    if (!identifier) {
      return null
    }
    return identifier.doi !== null ? 'DOI' : 'URI'
  }
}

export default new IdentifierMapper()
