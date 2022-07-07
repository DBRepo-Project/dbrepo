<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title>
          Modify Database
        </v-card-title>
        <v-card-text>
          <v-checkbox
            id="public"
            v-model="modify.is_public"
            name="public"
            label="Public" />
          <v-text-field
            id="publisher"
            v-model="modify.publisher"
            name="publisher"
            label="Publisher *"
            autofocus
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-textarea
            id="description"
            v-model="modify.description"
            name="description"
            rows="2"
            label="Description *"
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-select
            id="language"
            v-model="modify.language"
            name="language"
            label="Language *"
            :items="languages"
            item-value="value"
            item-text="text"
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-text-field
            id="publication-year"
            v-model.number="modify.publication_year"
            name="publication_year"
            label="Publication Year *"
            type="number"
            :rules="[v => !!v || $t('Required')]"
            required />
          <v-select
            id="license"
            v-model="modify.license"
            name="license"
            label="License *"
            :items="licenses"
            item-value="identifier"
            item-text="identifier"
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
            id="database"
            class="mb-2"
            :disabled="!valid || loading"
            color="primary"
            type="submit"
            @click="updateDatabase">
            Create
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>

export default {
  props: {
    database: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      valid: false,
      loading: false,
      error: false,
      modify: {
        is_public: null,
        publisher: null,
        description: null,
        language: null,
        publication_year: null,
        license: null
      },
      licenses: [],
      languages: [
        { text: 'EN', value: 'EN' },
        { text: 'DE', value: 'DE' },
        { text: 'OTHER', value: 'OTHER' }
      ]
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
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    }
  },
  mounted () {
    this.loadLicenses()
    this.modify.is_public = this.database.is_public
    this.modify.publisher = this.database.publisher
    this.modify.description = this.database.description
    this.modify.publication_year = this.database.publication_year
    this.modify.language = this.database.language
    this.modify.license = this.database.license
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog')
    },
    async loadLicenses () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/license`)
        this.licenses = res.data
        console.debug('licenses', this.licenses)
      } catch (err) {
        this.error = true
        this.$toast.error('Failed to fetch licenses.')
      }
      this.loading = false
    },
    async updateDatabase () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.modify, this.config)
        this.database = res.data
        console.debug('database', this.database)
        this.$toast.success('Successfully updated the database.')
      } catch (err) {
        this.error = true
        this.loading = false
        this.$toast.error('Failed to update database.')
        return
      }
      this.loading = false
      this.cancel()
    }
  }
}
</script>
