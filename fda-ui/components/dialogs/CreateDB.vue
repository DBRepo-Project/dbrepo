<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-progress-linear v-if="loading" v-model="progress" :color="loadingColor" />
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
            v-model="createContainer.name"
            name="database"
            label="Name *"
            autofocus
            :rules="[v => notEmpty(v) || $t('Required')]"
            required />
          <v-textarea
            id="description"
            v-model="createDatabase.description"
            name="description"
            rows="2"
            label="Description *"
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
            v-model="createDatabase.is_public"
            color="primary"
            :label="publicLabel"
            name="public" />
          <p>{{ summary }}</p>
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
            @click="createDB">
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
      progress: 0,
      createContainer: {
        name: null,
        repository: null,
        tag: null
      },
      createDatabase: {
        name: null,
        description: null,
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
      return this.createDatabase.is_public ? 'Public' : 'Private'
    },
    summary () {
      return 'Your database will be ' +
        (this.createDatabase.is_public ? 'publicly visible to the world' : 'visible only to you') +
        ' and run ' +
        (this.engine.repository === 'mariadb' ? 'MariaDB Engine (' + this.engine.tag + ')' : 'other')
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
      let res
      try {
        this.loading = true
        this.error = false
        res = await this.$axios.get('/api/image')
        this.engines = res.data
        console.debug('engines', this.engines)
        if (this.engines.length > 0) {
          this.engine = this.engines[0]
        }
        this.loading = false
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to fetch supported engines. Try reload the page.')
      }
      this.loading = false
    },
    sleep (ms) {
      return new Promise((resolve) => {
        setTimeout(resolve, ms)
      })
    },
    notEmpty,
    async createDB () {
      let res
      // create a container
      let containerId
      console.debug('model', this.engine)
      try {
        this.loading = true
        this.error = false
        this.createContainer.repository = this.engine.repository
        this.createContainer.tag = this.engine.tag
        res = await this.$axios.post('/api/container', this.createContainer, this.config)
        containerId = res.data.id
        console.debug('created container', res.data)
        this.progress = 25
      } catch (err) {
        this.error = true
        this.loading = false
        if (err.status === 401) {
          this.$toast.error('Authentication missing')
          console.error('permission denied', err)
          return
        }
        console.error('failed to create container', err)
        this.$toast.error('Could not create container.')
        return
      }

      // start the container
      try {
        this.loading = true
        this.error = false
        res = await this.$axios.put(`/api/container/${containerId}`, { action: 'start' }, this.config)
        console.debug('started container', res.data)
        this.progress = 50
      } catch (err) {
        this.error = true
        this.$toast.error('Could not start container.')
        return
      }

      // Pause.
      // DB fails to create when container has not started up yet
      await new Promise(resolve => setTimeout(resolve, 2000))

      // wait for it to finish
      this.loading = true
      this.error = false
      this.createDatabase.name = this.createContainer.name
      for (let i = 0; i < 5; i++) {
        try {
          res = await this.$axios.post(`/api/container/${containerId}/database`, this.createDatabase, this.config)
          console.debug('created database', res)
          break
        } catch (err) {
          console.debug('wait', res)
          this.progress += 10
          await this.sleep(3000)
        }
      }
      if (res.status !== 201) {
        this.loading = false
        this.error = true
        this.$toast.error('Could not create database.')
        return
      }
      this.progress = 100
      this.loading = false
      this.$toast.success(`Database "${res.data.name}" created.`)
      this.$emit('close')
      await this.$router.push(`/container/${containerId}/database/${res.data.id}/info`)
    }
  }
}
</script>
