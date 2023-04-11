import jwtDecode from 'jwt-decode'

class UserMapper {
  tokenToUser (token) {
    const data = jwtDecode(token)
    return {
      id: data.sub,
      firstname: data.given_name || null,
      lastname: data.family_name || null,
      username: data.client_id,
      roles: data.realm_access.roles || [],
      attributes: data.attributes || []
    }
  }

  tokenToRoles (token) {
    const data = jwtDecode(token)
    if (!data) {
      return []
    }
    return data.realm_access.roles || []
  }

  getThemeDark (user) {
    if (!user || !user.attributes || user.attributes.filter(a => a.name === 'theme_dark').length === 0) {
      return false
    }
    return user.attributes.filter(a => a.name === 'theme_dark')[0].value === 'true'
  }
}

export default new UserMapper()
