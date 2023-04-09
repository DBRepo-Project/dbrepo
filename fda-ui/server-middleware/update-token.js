import Vue from 'vue'

export default async function () {
  console.debug('loading token')
  try {
    await Vue.$keycloak.updateToken(70)
    return String(Vue.$keycloak.token)
  } catch (error) {
    console.error('Failed to update token', error)
  }
}
