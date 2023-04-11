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
import AuthenticationService from '@/api/authentication.service'
import UserService from '@/api/user.service'
import UserMapper from '@/api/user.mapper'
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
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    login () {
      this.loading = true
      AuthenticationService.authenticatePlain(this.username, this.password)
        .then(() => {
          const userId = UserMapper.tokenToUserId(this.token)
          UserService.findOne(userId)
            .then((user) => {
              this.$store.commit('SET_USER', user)
              this.$vuetify.theme.dark = UserMapper.getThemeDark(this.user)
              this.$router.push('/container')
            })
        })
        .catch(() => {
          this.loading = false
        })
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
