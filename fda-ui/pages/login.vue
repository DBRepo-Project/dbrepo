<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card v-if="!token" flat tile>
        <v-card-title>
          Login
        </v-card-title>
        <v-card-text>
          <v-alert
            border="left"
            color="info">
            If you need an account, <a @click="signup">create one</a> or if you cannot login, <a @click="forgot">reset</a> your information.
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
import { authenticate, refresh, tokenToExp, tokenToUser } from '@/api/user'
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
        console.debug('login user', res.data)
        // eslint-disable-next-line camelcase
        const { access_token, refresh_token } = res.data
        this.$store.commit('SET_TOKEN', access_token)
        this.$store.commit('SET_REFRESH_TOKEN', refresh_token)
        const user = tokenToUser(this.token)
        console.debug('user', user)
        this.$store.commit('SET_USER', user)
        this.$vuetify.theme.dark = user?.theme_dark || false
        await this.refreshTokenIfNecessary()
        await this.$router.push({ path: this.$route.query.redirect ? this.$route.query.redirect : '/container' })
      } catch (error) {
        console.error('Failed to login', error)
        const { status } = error.response
        if (status === 418) {
          this.$toast.error('Check your inbox and confirm your e-mail address')
          console.error('user has not confirmed e-mail', error)
        } else if (status === 404) {
          this.$toast.error('Username not found')
          console.error('user has not confirmed e-mail', error)
        } else {
          this.$toast.error('Login not successful')
          console.error('login user failed', error)
        }
        this.loading = false
      }
    },
    async refreshTokenIfNecessary () {
      if (!this.token) {
        return
      }
      const exp = tokenToExp(this.token)
      if (exp > new Date()) {
        console.debug('token will be refreshed', exp, 'timeout is', exp - new Date())
        setTimeout(() => this.refreshTokenIfNecessary(), exp - new Date())
        return
      }
      const refreshExp = tokenToExp(this.refreshToken)
      if (refreshExp > new Date()) {
        try {
          const res = await refresh(this.clientSecret, this.refreshToken)
          // eslint-disable-next-line camelcase
          const { access_token, refresh_token } = res.data
          this.$store.commit('SET_TOKEN', access_token)
          this.$store.commit('SET_REFRESH_TOKEN', refresh_token)
          console.info('refreshed tokens')
          const user = tokenToUser(this.token)
          console.debug('user', user)
          this.$store.commit('SET_USER', user)
          return
        } catch (error) {
          console.error('Failed to login', error)
          this.$toast.error('Failed to refresh tokens')
        }
      }
      this.logout('Your session has expired')
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
