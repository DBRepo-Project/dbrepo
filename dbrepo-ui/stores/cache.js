import { defineStore } from 'pinia'

export const useCacheStore = defineStore('cache', {
  persist: true,
  state: () => {
    return {
      database: null,
      table: null,
      view: null,
      subset: null,
      ontologies: [],
      messages: [],
      uploadProgress: null
    }
  },
  getters: {
    getDatabase: (state) => state.database,
    getTable: (state) => state.table,
    getView: (state) => state.view,
    getSubset: (state) => state.subset,
    getOntologies: (state) => state.ontologies,
    getMessages: (state) => state.messages,
    getUploadProgress: (state) => state.uploadProgress,
  },
  actions: {
    setDatabase(database) {
      this.database = database
    },
    setTable(table) {
      this.table = table
    },
    setView(view) {
      this.view = view
    },
    setSubset(subset) {
      this.subset = subset
    },
    setOntologies(ontologies) {
      this.ontologies = ontologies
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
    setRouteDatabase (databaseId) {
      return new Promise((resolve, reject) => {
        if (!databaseId) {
          this.database = null
          reject()
        }
        const databaseService = useDatabaseService()
        databaseService.findOne(databaseId, true)
          .then((database) => {
            this.database = database
            resolve(database)
          })
          .catch((error) => {
            this.database = null
            reject(error)
          })
      })
    },
    setRouteTable(databaseId, tableId) {
      if (!databaseId || !tableId) {
        this.table = null
        console.error('Cannot set route table: missing database id', databaseId, 'or table id', tableId)
        return
      }
      const tableService = useTableService()
      tableService.findOne(databaseId, tableId)
        .then(table => this.table = table)
    },
    setRouteView(databaseId, viewId) {
      if (!databaseId || !viewId) {
        this.view = null
        console.error('Cannot set route view: database view id', databaseId, 'or view id', viewId)
        return
      }
      const viewService = useViewService()
      viewService.findOne(databaseId, viewId)
        .then(view => this.view = view)
    },
    setRouteSubset(databaseId, subsetId) {
      if (!databaseId || !subsetId) {
        this.subset = null
        console.error('Cannot set route subset: missing database id', databaseId, 'or subset id', subsetId)
        return
      }
      const subsetService = useQueryService()
      subsetService.findOne(databaseId, subsetId)
        .then(subset => this.subset = subset)
    }
  },
})
