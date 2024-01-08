import UserMapper from '@/api/user.mapper'
const baseUrl = `${location.protocol}//${location.host}`

class DatabaseMapper {
  databaseToOwner (database) {
    if (!database) {
      return null
    }
    return UserMapper.userToFullName(database.owner)
  }

  databaseToContact (database) {
    if (!database) {
      return null
    }
    return UserMapper.userToFullName(database.contact)
  }

  databaseToJsonLd (database) {
    const jsonLd = {
      '@context': 'https://schema.org/',
      '@type': 'Dataset',
      name: database.name,
      description: 'Relational database hosted by the database repository.',
      url: `${baseUrl}/database/${database.id}/info`,
      isAccessibleForFree: database.is_public,
      creator: {
        '@type': 'Person'
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
}

export default new DatabaseMapper()
