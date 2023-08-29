import jwtDecode from 'jwt-decode'

class UserMapper {
  tokenToRoles (token) {
    const data = jwtDecode(token)
    return data.realm_access.roles || []
  }

  tokenToUserId (token) {
    const data = jwtDecode(token)
    return data.sub
  }

  userInfoToUser (data) {
    const obj = Object.assign({}, data)
    obj.attributes = {
      theme_dark: data.attributes.theme_dark,
      orcid: data.attributes.orcid,
      affiliation: data.attributes.affiliation
    }
    return obj
  }

  nameIdentifierToNameIdentifierScheme (nameIdentifier) {
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

  userToFullName (user) {
    if (!user) {
      return null
    }
    if (!('given_name' in user) || !('family_name' in user) || user.given_name === null || user.family_name === null) {
      return user.username
    }
    return user.given_name + ' ' + user.family_name
  }
}

export default new UserMapper()
