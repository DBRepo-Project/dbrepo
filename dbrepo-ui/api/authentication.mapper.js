import jwtDecode from 'jwt-decode'

class AuthenticationMapper {
  isExpiredToken (token) {
    return this.tokenToExpiryDate(token) < Date.now()
  }

  tokenToExpiryDate (token) {
    if (!token) {
      return true
    }
    const { exp } = jwtDecode(token)
    return new Date(exp * 1000)
  }
}

export default new AuthenticationMapper()
