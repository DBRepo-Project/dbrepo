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
          <v-row>
            <v-col sm="6">
              <v-text-field
                v-model="loginAccount.username"
                autocomplete="off"
                autofocus
                required
                :rules="[v => !!v || $t('Required')]"
                label="Username *" />
            </v-col>
          </v-row>
          <v-row>
            <v-col sm="6">
              <v-text-field
                v-model="loginAccount.password"
                autocomplete="off"
                type="password"
                required
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
export default {
  data () {
    return {
      loading: false,
      error: false, // XXX: `error` is never changed
      valid: false,
      loginAccount: {
        username: null,
        password: null
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
    user () {
      return this.$store.state.user
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
        const res = await this.$axios.post('/api/auth', this.loginAccount)
        console.debug('login user', res.data)
        const { token } = res.data
        this.$store.commit('SET_TOKEN', token)
        await this.setUser()
      } catch (error) {
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
    async setUser () {
      try {
        const res = await this.$axios.put('/api/auth', {}, this.config)
        this.$store.commit('SET_USER', res.data)
        this.$vuetify.theme.dark = res.data.theme_dark
        await this.$router.push(this.$route.query.redirect ? this.$route.query.redirect : '/container')
      } catch (error) {
        const { message } = error.response
        console.error('Failed to load user information', error)
        this.$toast.error('Failed to load user information: ' + message)
      }
      this.loading = false
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
