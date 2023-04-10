export function setToken (value) {
  localStorage.setItem('token', JSON.stringify(value))
}

export function getToken () {
  return JSON.parse(localStorage.getItem('token'))
}

export function setRefreshToken (value) {
  localStorage.setItem('refresh_token', JSON.stringify(value))
}

export function getRefreshToken () {
  return JSON.parse(localStorage.getItem('refresh_token'))
}

export function setUser (user) {
  localStorage.setItem('user', JSON.stringify(user))
}

export function getUser () {
  return JSON.parse(localStorage.getItem('user'))
}
