export function setToken (value) {
  const state = _getState()
  state.token = value
  _setState(state)
}

export function getToken () {
  const state = _getState()
  return state.token
}

export function setRefreshToken (value) {
  const state = _getState()
  state.refresh_token = value
  _setState(state)
}

export function getRefreshToken () {
  const state = _getState()
  return state.refresh_token
}

export function setUser (value) {
  const state = _getState()
  state.user = value
  _setState(state)
}

export function getUser () {
  const state = _getState()
  return state.user
}

export function _getState () {
  if (!JSON.parse(localStorage.getItem('vuex'))) {
    init()
  }
  return JSON.parse(localStorage.getItem('vuex'))
}

function _setState (state) {
  const json = JSON.stringify(state)
  localStorage.setItem('vuex', json)
}

function init () {
  const state = {
    token: null,
    roles: [],
    user: null,
    database: null,
    table: null,
    access: null
  }
  localStorage.setItem('vuex', JSON.stringify(state))
  console.debug('initialized vuex state')
}
