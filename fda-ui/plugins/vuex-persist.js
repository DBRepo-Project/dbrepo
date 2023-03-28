import { VuexPersistence } from 'vuex-persist'

export default ({ store }) => {
  new VuexPersistence({
    storage: window.localStorage,
    reducer: state => ({
      token: state.token,
      refreshToken: state.refreshToken,
      user: state.user
    })
  }).plugin(store)
}
