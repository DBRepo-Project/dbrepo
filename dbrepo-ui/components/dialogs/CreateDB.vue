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
                v-model="createDatabaseDto.name"
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
                :item-text="item => `${item.name}`"
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
      createDatabaseDto: {
        name: null,
        is_public: true
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
    this.getEngines()
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { success: false })
    },
    getEngines () {
      this.loading = true
      ContainerService.findAll()
        .then((containers) => {
          this.engines = containers
          if (this.engines.length > 0) {
            this.engine = this.engines[0]
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    async create () {
      this.loading = true
      try {
        this.database = await DatabaseService.create({ container_id: this.engine.id, name: this.createDatabaseDto.name, is_public: true })
        this.$emit('close', { success: true })
        return this.database
      } finally {
        this.loading = false
      }
    },
    notEmpty
  }
}
</script>
