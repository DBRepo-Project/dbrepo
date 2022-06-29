<template>
  <div>
    <v-card>
      <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
      <v-card-title>
        Persist Query and Result
      </v-card-title>
      <v-card-text>
        <v-alert
          border="left"
          color="info">
          Choose an expressive query title and describe what result the query produces.
        </v-alert>
        <v-form v-model="formValid" autocomplete="off">
          <v-row dense>
            <v-col>
              <v-text-field
                id="title"
                v-model="identifier.title"
                name="title"
                label="Query Title *"
                :rules="[v => !!v || $t('Required')]"
                required />
              <v-textarea
                id="description"
                v-model="identifier.description"
                name="description"
                rows="2"
                label="Query Description *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                id="publication_year"
                v-model.number="identifier.publication_year"
                type="number"
                label="Publication Year *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-select
                id="visibility"
                v-model="identifier.visibility"
                :items="visibility"
                item-value="value"
                :disabled="database.is_public"
                item-text="name"
                label="Visibility *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
          </v-row>
          <v-row v-for="(creator,i) in identifier.creators" :key="i" dense>
            <v-col cols="3">
              <v-text-field
                v-model="creator.name"
                name="name"
                label="Name *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="creator.affiliation"
                name="affiliation"
                label="Affiliation *"
                :rules="[v => !!v || $t('Required')]"
                required />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="creator.orcid"
                name="orcid"
                label="ORCID" />
            </v-col>
            <v-col cols="1" class="mt-5">
              <v-btn v-if="i !== 0" color="red darken-2" icon x-small @click="deleteCreator(i)">
                <v-icon>mdi-delete</v-icon>
              </v-btn>
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-btn x-small @click="addCreator">
                Add Creator
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
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
          class="mb-2"
          :disabled="!formValid || loading"
          color="primary"
          @click="persist">
          Persist
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      formValid: false,
      loading: false,
      error: false, // XXX: `error` is never changed
      visibility: [{
        name: 'Public',
        value: 'EVERYONE'
      },
      {
        name: 'Only me',
        value: 'SELF'
      }],
      database: {
        id: null,
        name: null,
        is_public: null,
        publisher: null
      },
      identifier: {
        cid: parseInt(this.$route.params.container_id),
        dbid: parseInt(this.$route.params.database_id),
        qid: parseInt(this.$route.params.query_id),
        title: null,
        description: null,
        publication_year: parseInt(new Date().getFullYear()),
        visibility: 'EVERYONE',
        doi: null,
        creators: []
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
    headers () {
      if (this.token === null) {
        return null
      }
      return { Authorization: `Bearer ${this.token}` }
    }
  },
  mounted () {
    this.loadUser()
    this.addCreator()
    this.loadDatabase()
  },
  methods: {
    cancel () {
      this.$parent.$parent.$parent.persistQueryDialog = false
      this.$emit('close', { action: 'closed' })
    },
    addCreator () {
      this.identifier.creators.push({
        name: null,
        affiliation: null,
        orcid: null
      })
    },
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, {
          headers: this.headers
        })
        console.debug('database', res.data)
        this.database = res.data
      } catch (err) {
        this.error = true
        console.error('Could not load database', err)
        this.$toast.error('Could not load database')
      }
      this.loading = false
    },
    deleteCreator (index) {
      this.identifier.creators.splice(index, 1)
    },
    async persist () {
      this.loading = true
      let res
      try {
        res = await this.$axios.post(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/identifier`, this.identifier, {
          headers: this.headers
        })
        console.debug('persist', res.data)
      } catch (err) {
        this.error = true
        this.loading = false
        this.$toast.error('Failed to persist query')
        console.error('persist failed', err)
        return
      }
      this.$toast.success('Query persisted.')
      this.$emit('close', { action: 'persisted' })
      this.loading = false
    },
    async loadUser () {
      this.loading = true
      let res
      try {
        res = await this.$axios.put('/api/auth', null, {
          headers: this.headers
        })
        console.debug('user data', res.data)
      } catch (err) {
        this.$toast.error('Failed load user data')
        console.error('load user data failed', err)
      }
      this.loading = false
    }
  }
}
</script>
<style>
#creators,
#creators-btn {
  background-color: #f00;
  margin-left: -16px !important;
  margin-right: -16px !important;
}
</style>
