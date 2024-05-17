import {jwtDecode} from 'jwt-decode'

export const useAuthenticationService = (): any => {

  function isExpiredToken(token: string): boolean {
    if (!token) {
      return false
    }
    return tokenToExpiryDate(token) < Date.now()
  }

  function tokenToExpiryDate(token: string): number {
    if (!token) {
      return -1
    }
    const exp: number = jwtDecode<Token>(token).exp
    if (exp) {
      return exp * 1000
    }
    return -1
  }

  return {isExpiredToken, tokenToExpiryDate}
}
