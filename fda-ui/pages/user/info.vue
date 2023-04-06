<template>
  <div v-if="token">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <div v-if="canModifyInformation">
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
                      :rules="[v => !!v || $t('Required')]"
                      required
                      label="Firstname *" />
                  </v-col>
                </v-row>
                <v-row dense>
                  <v-col md="6">
                    <v-text-field
                      v-model="model.lastname"
                      :rules="[v => !!v || $t('Required')]"
                      required
                      label="Lastname *" />
                  </v-col>
                </v-row>
                <v-row>
                  <v-col>
                    <v-btn
                      small
                      color="primary"
                      :loading="loading"
                      @click="updateInfo">
                      Update
                    </v-btn>
                  </v-col>
                </v-row>
                <!--              <v-row dense>-->
                <!--                <v-col md="6">-->
                <!--                  <v-text-field-->
                <!--                    v-model="model.affiliation"-->
                <!--                    disabled-->
                <!--                    hint="e.g. University of xyz"-->
                <!--                    label="Affiliation" />-->
                <!--                </v-col>-->
                <!--              </v-row>-->
                <!--              <v-row dense>-->
                <!--                <v-col md="6">-->
                <!--                  <v-text-field-->
                <!--                    v-model="model.orcid"-->
                <!--                    disabled-->
                <!--                    maxlength="19"-->
                <!--                    hint="e.g. 0000-0002-1825-0097"-->
                <!--                    label="ORCID" />-->
                <!--                </v-col>-->
                <!--              </v-row>-->
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
import { tokenToRoles, updateUser, toggleUserTheme, getThemeDark } from '@/api/user'

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
  watch: {
    user () {
      this.init()
    }
  },
  mounted () {
    this.init()
  },
  methods: {
    submit () {
    },
    async updateInfo () {
      try {
        this.loadingUpdate = true
        const payload = {
          firstname: this.model.firstname,
          lastname: this.model.lastname
        }
        const res = await updateUser(this.token, this.user.id, payload)
        console.info('Updated user information')
        const user = res.data
        console.debug('user', user)
        this.$store.commit('SET_USER', user)
        this.error = false
        this.$toast.success('Successfully updated user information')
      } catch (error) {
        console.error('update', error)
        this.$toast.error('Failed to update user info')
        this.error = true
      }
      this.loadingUpdate = false
    },
    async toggleTheme () {
      try {
        await toggleUserTheme(this.token, this.user.id, this.theme_dark)
        this.$vuetify.theme.dark = this.theme_dark
      } catch (error) {
        const { message } = error.response
        console.error('Failed to update theme', error)
        this.$toast.error('Failed to update theme: ' + message)
        this.error = true
      }
    },
    init () {
      if (!this.user) {
        return
      }
      this.theme_dark = getThemeDark(this.user)
      this.model.id = this.user.id
      this.model.username = this.user.username
      this.model.firstname = this.user.given_name
      this.model.lastname = this.user.family_name
    }
  }
}
</script>
