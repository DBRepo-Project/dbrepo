<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit" autocomplete="off">
      <v-card>
        <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
        <v-card-title v-text="database.name" />
        <v-card-subtitle>Modify Visibility</v-card-subtitle>
        <v-card-text>
          <v-alert
            border="left"
            color="error">
            <strong>Dangerous operation:</strong> you are about to change the visibility of the database. This affects all (sensitive) data held in the database. Please type the name of the database <strong v-text="database.internal_name" /> in the box below to confirm.
          </v-alert>
          <v-row dense>
            <v-col>
              <v-switch
                id="public"
                v-model="modify.is_public"
                color="primary"
                :label="publicLabel"
                name="public" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col>
              <v-text-field
                id="confirm"
                v-model="confirm"
                label="Database Name"
                name="confirm" />
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
            id="database"
            class="mb-2 mr-2"
            :disabled="!valid || loading || !confirmOk"
            color="error"
            type="submit"
            @click="updateDatabase">
            Update
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
      confirm: null,
      modify: {
        is_public: null
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
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    publicLabel () {
      return this.modify.is_public ? 'Public' : 'Private'
    },
    confirmOk () {
      return this.confirm === this.database.internal_name
    }
  },
  mounted () {
    this.modify.is_public = this.database.is_public
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close-dialog')
    },
    async updateDatabase () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/transfer`, this.modify, this.config)
        this.database = res.data
        console.debug('database', this.database)
        this.$toast.success('Successfully updated the database.')
        this.cancel()
        await this.$router.push(`/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/`)
      } catch (err) {
        this.error = true
        this.loading = false
        this.$toast.error('Failed to update database.')
        return
      }
      this.loading = false
    }
  }
}
</script>
