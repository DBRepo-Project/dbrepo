import { defineStore } from 'pinia'

export const useCacheStore = defineStore('cache', {
  persist: true,
  state: () => {
    return {
      database: null,
      table: null,
      ontologies: [],
      messages: [],
      uploadProgress: null
    }
  },
  getters: {
    getDatabase: (state) => state.database,
    getTable: (state) => state.table,
    getOntologies: (state) => state.ontologies,
    getMessages: (state) => state.messages,
    getUploadProgress: (state) => state.uploadProgress,
  },
  actions: {
    setDatabase (database) {
      this.database = database
    },
    setTable (table) {
      this.table = table
    },
    setOntologies (ontologies) {
      this.ontologies = ontologies
    },
    setUploadProgress (uploadProgress) {
      this.uploadProgress = uploadProgress
    },
    reloadMessages () {
      const messageService = useMessageService()
      messageService.findAll('active')
        .then(messages => this.messages = messages)
        .catch((error) => {
          console.error('Failed to reload messages', error)
        })
    },
    reloadOntologies () {
      const ontologyService = useOntologyService()
      ontologyService.findAll()
        .then(ontologies => this.ontologies = ontologies)
        .catch((error) => {
          console.error('Failed to reload ontologies', error)
        })
    },
    reloadDatabase () {
      const databaseService = useDatabaseService()
      databaseService.findOne(this.database.id)
        .then(database => this.database = database)
        .catch((error) => {
          console.error('Failed to reload database', error)
        })
    },
    reloadTable () {
      const tableService = useTableService()
      tableService.findOne(this.table.database_id, this.table.id)
        .then(table => this.table = table)
        .catch((error) => {
          console.error('Failed to reload table', error)
        })
    },
    setRouteDatabase (databaseId) {
      if (!databaseId) {
        this.database = null
        return
      }
      const databaseService = useDatabaseService()
      databaseService.findOne(databaseId)
        .then(database => this.database = database)
        .catch((error) => {
          console.error('Failed to set route database', error)
        })
    },
    setRouteTable (databaseId, tableId) {
      if (!databaseId || !tableId) {
        this.table = null
        console.error('Cannot set route table: missing database id', databaseId, 'or table id', tableId)
        return
      }
      const tableService = useTableService()
      tableService.findOne(databaseId, tableId)
        .then(table => this.table = table)
        .catch((error) => {
          console.error('Failed to set route table', error)
        })
    }
  },
})
