<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
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
          <v-switch
            id="public"
            v-model="createDatabaseDto.is_public"
            color="primary"
            :label="publicLabel"
            name="public" />
          <p v-if="createDatabaseDto.is_public">
            Your database tables will be <strong>publicly visible</strong>. The metadata is also publicly visible to the
            world. It will run the engine <strong v-text="`${engine.repository}:${engine.tag}`" />.
          </p>
          <p v-if="!createDatabaseDto.is_public">
            Your database tables will be <strong>private</strong>. The metadata will still be <strong>publicly visible</strong>
            to the world. It will run the engine <strong v-text="`${engine.repository}:${engine.tag}`" />.
          </p>
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
      container: {
        id: null,
        name: null
      },
      database: {
        id: null
      },
      createDatabaseDto: {
        name: null,
        is_public: true
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
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    publicLabel () {
      return this.createDatabaseDto.is_public ? 'Public' : 'Private'
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
      this.$parent.$parent.$parent.$parent.createDbDialog = false
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
    async createContainer () {
      this.createContainerDto.repository = this.engine.repository
      this.createContainerDto.tag = this.engine.tag
      try {
        this.loading = true
        const res = await this.$axios.post('/api/container', this.createContainerDto, this.config)
        this.container = res.data
        console.debug('created container', this.container)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to create container')
      }
      this.loading = false
    },
    async startContainer () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${this.container.id}`, { action: 'start' }, this.config)
        console.debug('started container', res.data)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to start container')
      }
      this.loading = false
    },
    async inspectContainer () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.container.id}`, this.config)
        const { state } = res.data
        console.debug('inspected container', res.data)
        if (state !== 'running') {
          this.error = true
          console.error('Container is not running')
        }
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to start container')
      }
      this.loading = false
    },
    async createDatabase () {
      try {
        this.loading = true
        this.createDatabaseDto.name = this.container.name
        const res = await this.$axios.post(`/api/container/${this.container.id}/database`, this.createDatabaseDto, this.config)
        this.database = res.data
        console.debug('created database', this.database)
        await this.$router.push(`/container/${this.container.id}/database/${this.database.id}/info`)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to create database')
      }
      this.loading = false
    },
    async deleteContainer () {
      try {
        this.loading = true
        await this.$axios.delete(`/api/container/${this.container.id}`, this.config)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to delete container')
      }
      this.loading = false
    },
    notEmpty,
    create () {
      this.createContainer()
        .then(() => this.startContainer()
          .then(() => this.inspectContainer()
            .then(() => this.createDatabase())))
        .catch((err) => {
          console.error('Failed to create database, rollback container', err)
          this.deleteContainer()
        })
    }
  }
}
</script>
