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
      if (creators[i].firstname) {
        str += (creators[i].firstname.toUpperCase().substring(0, 1) + '., ')
      }
      if (creators[i].lastname) {
        str += creators[i].lastname
      }
    }
    return str
  }

  identifierToIdentifierSave (data) {
    return {
      database_id: data.database_id,
      query_id: data.query_id,
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
      license: data.license,
      creators: data.creators.map((c) => {
        return {
          id: c.id,
          firstname: c.firstname,
          lastname: c.lastname,
          creator_name: c.creator_name,
          name_type: c.name_type,
          name_identifier: c.name_identifier,
          name_identifier_scheme: c.name_identifier_scheme,
          affiliation: c.affiliation,
          affiliation_identifier: c.affiliation_identifier
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
}

export default new IdentifierMapper()
