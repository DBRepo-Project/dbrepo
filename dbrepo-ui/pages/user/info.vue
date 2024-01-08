<template>
  <div v-if="token">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <div>
          <v-card flat>
            <v-card-title>User Information</v-card-title>
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
                      :disabled="!canModifyInformation"
                      label="Firstname" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="6">
                    <v-text-field
                      v-model="model.lastname"
                      :disabled="!canModifyInformation"
                      label="Lastname" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="6">
                    <v-text-field
                      v-model="model.affiliation"
                      :disabled="!canModifyInformation"
                      hint="e.g. University of xyz"
                      label="Affiliation" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="6">
                    <v-text-field
                      v-model="model.orcid"
                      :disabled="!canModifyInformation"
                      maxlength="37"
                      hint="e.g. https://orcid.org/0000-0002-1825-0097"
                      label="ORCID" />
                  </v-col>
                </v-row>
                <v-row>
                  <v-col>
                    <v-btn
                      small
                      :disabled="!canModifyInformation"
                      color="primary"
                      :loading="loading"
                      @click="updateInfo">
                      Update
                    </v-btn>
                  </v-col>
                </v-row>
              </v-form>
            </v-card-text>
          </v-card>
        </div>
        <div v-if="canModifyTheme">
          <v-divider />
          <v-card flat>
            <v-card-title>Theme</v-card-title>
            <v-card-text>
              <v-row dense>
                <v-col>
                  <v-switch
                    v-model="theme_dark"
                    :loading="loadingUpdate"
                    inset
                    :label="themeLabel"
                    @click="toggleTheme" />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </div>
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import UserToolbar from '@/components/UserToolbar'
import UserService from '@/api/user.service'

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
      loadingUpdate: false,
      model: {
        id: null,
        username: null,
        firstname: null,
        lastname: null
      },
      theme_dark: null
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
      return this.$store.state.roles
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    themeLabel () {
      return `${this.theme_dark ? 'Dark' : 'Light'} Theme`
    },
    canModifyTheme () {
      return this.roles.includes('modify-user-theme')
    },
    canModifyInformation () {
      return this.roles.includes('modify-user-information')
    }
  },
  mounted () {
    this.init()
  },
  methods: {
    submit () {
    },
    updateInfo () {
      this.loadingUpdate = true
      const payload = {
        firstname: this.model.firstname,
        lastname: this.model.lastname,
        orcid: this.model.orcid,
        affiliation: this.model.affiliation
      }
      UserService.updateInformation(this.user.id, payload)
        .then(() => {
          console.info('Updated user information')
          this.$toast.success('Successfully updated user information')
          this.reloadUser()
        })
        .catch(() => {
          this.loadingUpdate = false
        })
        .finally(() => {
          this.loadingUpdate = false
        })
    },
    reloadUser () {
      this.$store.dispatch('reloadUser')
    },
    toggleTheme () {
      UserService.updateTheme(this.user.id, this.theme_dark)
        .then(() => {
          this.reloadUser()
          this.$vuetify.theme.dark = this.theme_dark
        })
    },
    init () {
      if (!this.user) {
        console.warn('Object user is not yet available')
        return
      }
      this.reloadUser()
      this.theme_dark = this.user.attributes.theme_dark
      this.model = {
        id: this.user.id,
        username: this.user.username,
        firstname: this.user.given_name,
        lastname: this.user.family_name,
        orcid: this.user.attributes.orcid,
        affiliation: this.user.attributes.affiliation
      }
    }
  }
}
</script>
