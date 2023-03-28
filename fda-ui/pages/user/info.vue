<template>
  <div v-if="token">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat>
          <v-card-title>User Information</v-card-title>
          <v-card-subtitle>Your identity is externally managed</v-card-subtitle>
          <v-card-text>
            <v-form v-model="valid1" @submit.prevent="submit">
              <v-row dense>
                <v-col md="3">
                  <v-text-field
                    v-model="model.id"
                    disabled
                    label="ID" />
                </v-col>
                <v-col md="3">
                  <v-text-field
                    v-model="model.username"
                    disabled
                    label="Username" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.firstname"
                    disabled
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Firstname *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.lastname"
                    disabled
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Lastname *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.affiliation"
                    disabled
                    hint="e.g. University of xyz"
                    label="Affiliation" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.orcid"
                    disabled
                    maxlength="19"
                    hint="e.g. 0000-0002-1825-0097"
                    label="ORCID" />
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
          <v-divider />
          <v-card-title>Roles</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col>
                <pre>{{ roles.join(', ') }}</pre>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import UserToolbar from '@/components/UserToolbar'
import { tokenToRoles } from '@/api/user'

export default {
  components: {
    UserToolbar
  },
  data () {
    return {
      tab: 0,
      valid1: false,
      valid2: false,
      error: false,
      loading: false,
      model: {
        id: null,
        username: null,
        firstname: null,
        lastname: null,
        titles_before: null,
        titles_after: null,
        affiliation: null,
        orcid: null,
        theme_dark: null
      }
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      const roles = tokenToRoles(this.token)
      console.debug('roles', roles)
      return roles
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
    this.model = Object.assign({}, this.user)
  },
  methods: {
    submit () {
    },
    async updateInfo () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/user/${this.user.id}`, {
          titles_before: this.model.titles_before,
          titles_after: this.model.titles_after,
          firstname: this.model.firstname,
          lastname: this.model.lastname,
          affiliation: this.model.affiliation,
          orcid: this.model.orcid
        }, this.config)
        console.info('Updated user information')
        console.debug('user information', res.data)
        this.error = false
        this.$toast.success('Successfully updated user information')
      } catch (err) {
        console.error('update', err)
        this.$toast.error('Failed to update user info')
        this.error = true
      }
      this.loading = false
    },
    async toggleTheme () {
      try {
        await this.$axios.put(`/api/user/${this.user.id}/theme`, {
          theme_dark: this.model.theme_dark
        }, this.config)
        this.$vuetify.theme.dark = this.model.theme_dark
        console.info('Set theme to', this.model.theme_dark ? 'dark' : 'light')
      } catch (error) {
        const { message } = error.response
        console.error('Failed to update theme', error)
        this.$toast.error('Failed to update theme: ' + message)
        this.error = true
      }
    }
  }
}
</script>
