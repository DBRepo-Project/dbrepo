import Vue from 'vue'
import Keycloak from 'keycloak-js'
import api from '../config'

const initOptions = {
  url: api + '/api/auth',
  realm: 'dbrepo',
  clientId: 'dbrepo-client'
}

const _keycloak = new Keycloak(initOptions)

const Plugin = {
  install: (Vue) => {
    Vue.$keycloak = _keycloak
  }
}
Plugin.install = (Vue) => {
  Vue.$keycloak = _keycloak
  Object.defineProperties(Vue.prototype, {
    $keycloak: {
      get () {
        return _keycloak
      }
    }
  })
}

Vue.use(Plugin)

export default Plugin
