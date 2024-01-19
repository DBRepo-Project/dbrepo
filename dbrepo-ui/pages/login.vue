<template>
  <div>
    <v-toolbar v-if="!token" flat>
      <v-toolbar-title>
        Login
      </v-toolbar-title>
    </v-toolbar>
    <v-form v-if="!token" ref="form" v-model="valid" @submit.prevent="submit">
      <v-card flat tile>
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
        <v-card-subtitle class="text-right">
          <a v-for="(link, i) in loginLinks" :key="i" class="ml-1" :href="link.href" :target="link.blank ? '_blank' : 'self'">
            {{ link.text }} <sup v-if="link.blank"><v-icon color="primary" x-small>mdi-open-in-new</v-icon></sup>
          </a>
        </v-card-subtitle>
      </v-card>
    </v-form>
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
    token () {
      return this.$store.state.token
    },
    refreshToken () {
      return this.$store.state.refreshToken
    },
    user () {
      return this.$store.state.user
    },
    loginLinks () {
      const loginLinks = this.$config.loginLinks
      console.debug('login links', loginLinks)
      return loginLinks
    }
  },
  mounted () {
    if (this.token) {
      this.$router.push('/database')
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
            .then(async (user) => {
              this.$store.commit('SET_USER', user)
              this.$vuetify.theme.dark = user.attributes.theme_dark
              await this.$router.push('/database')
            })
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    signup () {
      this.$router.push('/signup')
    }
  }
}
</script>
