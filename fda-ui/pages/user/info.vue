<template>
  <div>
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat>
          <v-card-title>User Information</v-card-title>
          <v-card-text>
            <v-form v-model="valid1" @submit.prevent="submit">
              <v-row dense>
                <v-col cols="2">
                  <v-text-field
                    v-model="user.id"
                    disabled
                    label="ID" />
                </v-col>
                <v-col cols="3">
                  <v-text-field
                    v-model="user.username"
                    disabled
                    label="Username" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="user.titles_before"
                    :disabled="error"
                    hint="e.g. Prof."
                    label="Titles Before" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="user.firstname"
                    :disabled="error"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Firstname *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="user.lastname"
                    :disabled="error"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Lastname *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="user.titles_after"
                    :disabled="error"
                    hint="e.g. BSc"
                    label="Titles After" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="user.affiliation"
                    :disabled="error"
                    hint="e.g. University of xyz"
                    label="Affiliation" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="user.orcid"
                    :disabled="error"
                    maxlength="19"
                    hint="e.g. 0000-0002-1825-0097"
                    label="ORCID" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-btn
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
                    v-model="user.theme_dark"
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
export default {
  data () {
    return {
      tab: 0,
      valid1: false,
      valid2: false,
      error: false,
      loading: false,
      user: {
        id: null,
        email: null,
        username: null,
        lastname: null,
        firstname: null,
        titles_after: null,
        titles_before: null,
        email_verified: false,
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
    this.loadUser()
  },
  methods: {
    submit () {
    },
    async loadUser () {
      if (!this.token) {
        return
      }
      try {
        this.loading = true
        const res = await this.$axios.put('/api/auth/', {}, this.config)
        this.user = res.data
        console.debug('user', this.user)
        this.error = false
      } catch (err) {
        this.$toast.error('Failed to load user')
        console.error('user', err)
        this.error = true
      }
      this.loading = false
    },
    async updateInfo () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/user/${this.user.id}`, {
          titles_before: this.user.titles_before,
          titles_after: this.user.titles_after,
          firstname: this.user.firstname,
          lastname: this.user.lastname,
          affiliation: this.user.affiliation,
          orcid: this.user.orcid
        }, this.config)
        console.debug('update', res.data)
        this.error = false
        this.$toast.success('Successfully updated user info')
      } catch (err) {
        console.error('update', err)
        this.$toast.error('Failed to update user info')
        this.error = true
      }
      this.loading = false
    },
    async toggleTheme () {
      if (this.loading) {
        return
      }
      try {
        this.loadingTheme = true
        const res = await this.$axios.put(`/api/user/${this.user.id}/theme`, {
          theme_dark: this.user.theme_dark
        }, this.config)
        console.debug('theme set', res.data)
        this.$vuetify.theme.dark = this.user.theme_dark
      } catch (err) {
        console.error('theme set', err)
        this.$toast.error('Failed to update theme')
        this.error = true
      }
      this.loadingTheme = false
    }
  }
}
</script>
