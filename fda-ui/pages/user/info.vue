<template>
  <div v-if="token">
    <pre>{{ model }}</pre>
    <pre>{{ user }}</pre>
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat>
          <v-card-title>User Information</v-card-title>
          <v-card-text>
            <v-form v-model="valid1" @submit.prevent="submit">
              <v-row dense>
                <v-col md="2">
                  <v-text-field
                    v-model="model.id"
                    disabled
                    label="ID" />
                </v-col>
                <v-col md="4">
                  <v-text-field
                    v-model="model.username"
                    disabled
                    label="Username" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="roles"
                    disabled
                    label="Roles" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.titles_before"
                    :disabled="error"
                    hint="e.g. Prof."
                    label="Titles Before" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.firstname"
                    :disabled="error"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Firstname *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.lastname"
                    :disabled="error"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Lastname *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.titles_after"
                    :disabled="error"
                    hint="e.g. BSc"
                    label="Titles After" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.affiliation"
                    :disabled="error"
                    hint="e.g. University of xyz"
                    label="Affiliation" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.orcid"
                    :disabled="error"
                    maxlength="19"
                    hint="e.g. 0000-0002-1825-0097"
                    label="ORCID" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-btn
                    small
                    color="primary"
                    :disabled="!valid1 || error"
                    type="submit"
                    @click="updateInfo">
                    Update
                  </v-btn>
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
          <v-divider />
          <v-card-title>Theme</v-card-title>
          <v-card-text>
            <v-form v-model="valid2" @submit.prevent="submit">
              <v-row dense>
                <v-col cols="5">
                  <v-switch
                    v-model="model.theme_dark"
                    inset
                    label="Dark Mode"
                    :disabled="error"
                    :loading="loading"
                    @click="toggleTheme" />
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import UserToolbar from '@/components/UserToolbar'

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
      if (!this.user.roles) {
        return null
      }
      return this.user.roles.join(', ')
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
