import { VuexPersistence } from 'vuex-persist'

export default ({ store }) => {
  new VuexPersistence({
    storage: window.localStorage,
    reducer: state => ({
      title: state.title,
      icon: state.icon,
      token: state.token,
      refreshToken: state.refreshToken,
      roles: state.roles,
      user: state.user,
      database: state.database,
      table: state.table,
      access: state.access,
      locale: state.locale,
      messages: state.messages,
      ontologies: state.ontologies,
      clientId: state.clientId,
      clientSecret: state.clientSecret,
      searchUsername: state.searchUsername,
      searchPassword: state.searchPassword,
      databaseCount: state.databaseCount,
      doiUrl: state.doiUrl
    })
  }).plugin(store)
}
