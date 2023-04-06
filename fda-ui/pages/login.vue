<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        Login
      </v-toolbar-title>
    </v-toolbar>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card v-if="!token" flat tile>
        <v-card-text>
          <v-alert
            border="left"
            color="info">
            If you need an account, <a @click="signup">create one</a>.
          </v-alert>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="username"
                autocomplete="off"
                autofocus
                required
                name="username"
                :rules="[v => !!v || $t('Required')]"
                label="Username *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="password"
                autocomplete="off"
                type="password"
                required
                name="password"
                :rules="[v => !!v || $t('Required')]"
                label="Password *" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            id="login"
            class="mb-2 ml-2"
            :disabled="!valid"
            color="primary"
            type="submit"
            name="submit"
            :loading="loading"
            @click="login">
            Login
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
    <p v-if="token">Already logged-in</p>
  </div>
</template>

<script>
import { authenticate, getThemeDark, findUser, tokenToRoles } from '@/api/user'
export default {
  data () {
    return {
      loading: false,
      error: false, // XXX: `error` is never changed
      valid: false,
      username: null,
      password: null
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    token () {
      return this.$store.state.token
    },
    refreshToken () {
      return this.$store.state.refreshToken
    },
    user () {
      return this.$store.state.user
    },
    clientSecret () {
      return this.$config.clientSecret
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
    if (this.$route.query.email_verified !== undefined) {
      console.info('Successfully verified your E-Mail Address')
      this.$toast.success('Successfully verified your E-Mail Address!')
    } else if (this.$route.query.password_reset !== undefined) {
      console.info('Successfully reset password')
      this.$toast.success('Successfully reset password!')
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    async login () {
      try {
        this.loading = true
        const res = await authenticate(this.clientSecret, this.username, this.password)
        // eslint-disable-next-line camelcase
        const { access_token } = res.data
        this.$store.commit('SET_TOKEN', access_token)
        const roles = tokenToRoles(access_token)
        this.$store.commit('SET_ROLES', roles)
        await this.setTheme()
        await this.$router.push({ path: this.$route.query.redirect ? this.$route.query.redirect : '/container' })
      } catch (error) {
        console.error('Failed to login', error)
        const { statusText } = error.response
        this.$toast.error(`Failed to login: ${statusText}`)
        this.loading = false
      }
    },
    async setTheme () {
      try {
        const res = await findUser(this.token)
        const user = res.data
        console.debug('user', user)
        this.$store.commit('SET_USER', user)
        this.$vuetify.theme.dark = getThemeDark(user)
      } catch (error) {
        console.error('Failed to set theme', error)
      }
    },
    signup () {
      this.$router.push('/signup')
    },
    forgot () {
      this.$router.push('/forgot')
    }
  }
}
</script>
