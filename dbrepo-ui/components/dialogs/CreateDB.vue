<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-card-title>
          Create Database
        </v-card-title>
        <v-card-text>
          <v-alert
            border="left"
            color="info">
            Choose an expressive database name and select a database engine.
          </v-alert>
          <v-text-field
            id="database"
            v-model="createContainerDto.name"
            name="database"
            label="Name *"
            autofocus
            :rules="[v => notEmpty(v) || $t('Required')]"
            required />
          <v-select
            id="engine"
            v-model="engine"
            name="engine"
            label="Engine *"
            :items="engines"
            :item-text="item => `${item.repository}:${item.tag}`"
            :rules="[v => !!v || $t('Required')]"
            return-object
            required />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            class="mb-2"
            @click="cancel">
            Cancel
          </v-btn>
          <v-btn
            id="createDB"
            class="mb-2 mr-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            :loading="loading"
            @click="create">
            Create
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
const { notEmpty } = require('@/utils')

export default {
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      engine: {
        repository: null,
        tag: null
      },
      engines: [],
      createContainerDto: {
        name: null,
        repository: null,
        tag: null
      },
      createDatabaseDto: {
        name: null,
        is_public: true
      },
      container: {
        id: null,
        name: null
      },
      database: {
        id: null
      }
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    config () {
      if (this.token === null) {
        return {
          headers: {}
        }
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` },
        progress: false
      }
    },
    user () {
      return this.$store.state.user
    }
  },
  mounted () {
    this.getImages()
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
    async getImages () {
      try {
        this.loading = true
        const res = await this.$axios.get('/api/image')
        this.engines = res.data
        console.debug('engines', this.engines)
        if (this.engines.length > 0) {
          this.engine = this.engines[0]
        }
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to fetch supported engines. Try reload the page')
      }
      this.loading = false
    },
    async create () {
      await this.createContainer()
        .then(() => this.startContainer(this.container)
          .then(() => this.createDatabase(this.container)))
    },
    async createContainer () {
      this.createContainerDto.repository = this.engine.repository
      this.createContainerDto.tag = this.engine.tag
      try {
        this.loading = true
        const res = await this.$axios.post('/api/container', this.createContainerDto, this.config)
        this.container = res.data
        console.debug('created container', this.container)
        this.error = false
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to create container')
      }
      this.loading = false
    },
    async startContainer (container) {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${container.id}`, { action: 'start' }, this.config)
        console.debug('started container', res.data)
        this.error = false
      } catch (error) {
        const { status } = error.response
        if (status !== 409) {
          this.error = true
          this.$toast.error('Failed to start container')
        }
      }
      this.loading = false
    },
    async createDatabase (container) {
      try {
        this.loading = true
        this.createDatabaseDto.name = container.name
        const res = await this.$axios.post(`/api/container/${container.id}/database`, this.createDatabaseDto, this.config)
        container.database = res.data
        console.debug('created database', container.database)
        this.error = false
        this.$emit('close', { success: true })
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to create database')
      }
      this.loading = false
    },
    notEmpty
  }
}
</script>
