import { defineStore } from 'pinia'

export const useCacheStore = defineStore('cache', {
  persist: true,
  state: () => {
    return {
      resourceErrors: {},
      routeRequests: {},
      database: null,
      table: null,
      view: null,
      access: null,
      subset: null,
      locale: null,
      identifier: null,
      ontologies: [],
      messages: [],
      user: null,
      roles: [],
      dashboardSyncFailures: {},
      uploadProgress: null
    }
  },
  getters: {
    getDatabase: (state) => state.database,
    getTable: (state) => state.table,
    getView: (state) => state.view,
    getAccess: (state) => state.access,
    getSubset: (state) => state.subset,
    getLocale: (state) => state.locale,
    getIdentifier: (state) => state.identifier,
    getOntologies: (state) => state.ontologies,
    getMessages: (state) => state.messages,
    getUser: (state) => state.user,
    getRoles: (state) => state.roles,
    getUploadProgress: (state) => state.uploadProgress,
  },
  actions: {
    async loadRouteResource (kind, databaseId, resourceId, service) {
      const request = (this.routeRequests[kind] || 0) + 1
      this.routeRequests[kind] = request
      this[kind] = null
      this.resourceErrors[kind] = null
      if (!databaseId || !resourceId) return
      try {
        const resource = await service.findOne(databaseId, resourceId)
        if (this.routeRequests[kind] === request) this[kind] = resource
      } catch (error) {
        if (this.routeRequests[kind] === request) this.resourceErrors[kind] = error || true
      }
    },
    setDatabase(database) {
      this.database = database
    },
    setTable(table) {
      this.routeRequests.table = (this.routeRequests.table || 0) + 1
      this.resourceErrors.table = null
      this.table = table
    },
    setView(view) {
      this.routeRequests.view = (this.routeRequests.view || 0) + 1
      this.resourceErrors.view = null
      this.view = view
    },
    setAccess(access) {
      this.routeRequests.access = (this.routeRequests.access || 0) + 1
      this.access = access
    },
    setSubset(subset) {
      this.subset = subset
    },
    setLocale(locale) {
      this.locale = locale
    },
    setIdentifier(identifier) {
      this.identifier = identifier
    },
    setOntologies(ontologies) {
      this.ontologies = ontologies
    },
    setUser(user) {
      this.user = user
    },
    setRoles(roles) {
      this.roles = roles
    },
    setUploadProgress(uploadProgress) {
      this.uploadProgress = uploadProgress
    },
    reloadMessages() {
      const messageService = useMessageService()
      messageService.findAll('active')
        .then(messages => this.messages = messages)
        .catch((error) => {
          console.error('Failed to reload messages', error)
        })
    },
    reloadOntologies() {
      const ontologyService = useOntologyService()
      ontologyService.findAll()
        .then(ontologies => this.ontologies = ontologies)
        .catch((error) => {
          console.error('Failed to reload ontologies', error)
        })
    },
    reloadDatabase() {
      const databaseService = useDatabaseService()
      databaseService.findOne(this.database.id)
        .then(database => this.database = database)
        .catch((error) => {
          console.error('Failed to reload database', error)
        })
    },
    reloadTable() {
      const tableService = useTableService()
      tableService.findOne(this.table.database_id, this.table.id)
        .then(table => this.table = table)
        .catch((error) => {
          console.error('Failed to reload table', error)
        })
    },
    reloadView() {
      const viewService = useViewService()
      viewService.findOne(this.table.database_id, this.view.id)
        .then(view => this.view = view)
        .catch((error) => {
          console.error('Failed to reload view', error)
        })
    },
    reloadSubset() {
      const queryService = useQueryService()
      queryService.findOne(this.subset.database_id, this.subset.id)
        .then(subset => this.subset = subset)
        .catch((error) => {
          console.error('Failed to reload subset', error)
        })
    },
    async setRouteDatabase (databaseId) {
      const request = (this.routeRequests.database || 0) + 1
      this.routeRequests.database = request
      this.database = null
      if (!databaseId) return
      try {
        const database = await useDatabaseService().findOne(databaseId, true)
        if (this.routeRequests.database === request) this.database = database
        return database
      } catch (error) {
        if (this.routeRequests.database === request) throw error
      }
    },
    setRouteTable(databaseId, resourceId) {
      return this.loadRouteResource('table', databaseId, resourceId, useTableService())
    },
    async setRouteAccess(databaseId, username) {
      const request = (this.routeRequests.access || 0) + 1
      this.routeRequests.access = request
      this.access = null
      if (!databaseId || !username) return
      try {
        const access = await useAccessService().findOne(databaseId, username)
        if (this.routeRequests.access === request) this.access = access
      } catch {
        // No access grant: public resources remain readable.
      }
    },

    setRouteView(databaseId, resourceId) {
      return this.loadRouteResource('view', databaseId, resourceId, useViewService())
    },
    setRouteSubset(databaseId, subsetId) {
      if (!databaseId || !subsetId) {
        this.subset = null
        return
      }
      const subsetService = useQueryService()
      subsetService.findOne(databaseId, subsetId)
        .then(subset => this.subset = subset)
    }
  },
})
