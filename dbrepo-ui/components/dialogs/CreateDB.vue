<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card>
        <v-card-title>Create Database</v-card-title>
        <v-card-subtitle>Choose an expressive database name and select a database engine.</v-card-subtitle>
        <v-card-text>
          <v-row dense>
            <v-col>
              <v-text-field
                id="database"
                v-model="createContainerDto.name"
                name="database"
                label="Name *"
                autofocus
                :rules="[v => notEmpty(v) || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
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
            </v-col>
          </v-row>
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
import { notEmpty } from '@/utils'
import ContainerService from '@/api/container.service'
import DatabaseService from '@/api/database.service'

export default {
  data () {
    return {
      valid: false,
      loading: false,
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
    token () {
      return this.$store.state.token
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
    getImages () {
      this.loading = true
      ContainerService.findAllImages()
        .then((images) => {
          this.engines = images
          if (this.engines.length > 0) {
            this.engine = this.engines[0]
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    async create () {
      await this.createContainer()
        .then(() => this.startContainer(this.container)
          .then(() => this.createDatabase(this.container)))
    },
    createContainer () {
      this.createContainerDto.repository = this.engine.repository
      this.createContainerDto.tag = this.engine.tag
      this.loading = true
      return new Promise((resolve, reject) => {
        ContainerService.create(this.createContainerDto)
          .then((container) => {
            this.container = container
            this.loading = false
            resolve(container)
          })
          .catch(error => reject(error))
      })
    },
    startContainer (container) {
      this.loading = true
      return new Promise((resolve, reject) => {
        ContainerService.modify(container.id, 'start')
          .then(() => {
            this.loading = false
            resolve()
          })
          .catch(error => reject(error))
      })
    },
    createDatabase (container) {
      this.loading = true
      DatabaseService.create(container.id, { name: container.name, is_public: true })
        .then((database) => {
          container.database = database
          this.$emit('close', { success: true })
        })
        .finally(() => {
          this.loading = false
        })
    },
    notEmpty
  }
}
</script>
