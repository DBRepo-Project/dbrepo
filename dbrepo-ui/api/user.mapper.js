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
      theme_dark: data.attributes.filter(a => a.name === 'theme_dark')[0].value === 'true',
      orcid: data.attributes.filter(a => a.name === 'orcid')[0].value,
      affiliation: data.attributes.filter(a => a.name === 'affiliation')[0].value
    }
    return obj
  }

  userToFullName (user) {
    if (!user) {
      return null
    }
    if (!('given_name' in user) || !('family_name' in user) || user.given_name === null || user.family_name === null) {
      return user?.username
    }
    return user.given_name + ' ' + user.family_name
  }
}

export default new UserMapper()
