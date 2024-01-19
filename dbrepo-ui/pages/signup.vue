<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        Signup
      </v-toolbar-title>
    </v-toolbar>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card flat tile>
        <v-card-text>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="createAccount.email"
                type="email"
                autocomplete="off"
                autofocus
                required
                name="email"
                :rules="[v => !!v || $t('Required')]"
                hint="e.g. max.mustermann@work.com"
                label="Work E-Mail Address *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="createAccount.username"
                autocomplete="off"
                required
                name="username"
                :rules="[v => !!v || $t('Required'),
                         v => /^[a-z0-9]{3,}$/.test(v) || $t('Only lowercase letters, min. 3 length'),
                         v => !usernames.includes(v) || $t('This username is already taken')]"
                hint="e.g. mmustermann"
                label="Username *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="createAccount.password"
                autocomplete="off"
                required
                name="password"
                :rules="[v => !!v || $t('Required')]"
                type="password"
                label="Password *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="password2"
                autocomplete="off"
                required
                name="password-confirm"
                :rules="[v => !!v || $t('Required'), v => (!!v && v) === createAccount.password || $t('Not matching!')]"
                type="password"
                label="Repeat Password *" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-btn
            id="login"
            :disabled="!valid"
            color="primary"
            type="submit"
            name="submit"
            :loading="loading"
            @click="register">
            Submit
          </v-btn>
        </v-card-text>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import UserService from '@/api/user.service'
export default {
  data () {
    return {
      loading: false,
      loadingUsers: false,
      usernames: [],
      error: false, // XXX: `error` is never changed
      valid: false,
      password2: null,
      privacy: false,
      consent: false,
      createAccount: {
        username: null,
        email: null,
        password: null
      }
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    }
  },
  mounted () {
    this.loadUsers()
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    register () {
      this.loading = true
      UserService.create(this.createAccount)
        .then(() => {
          this.$toast.success('Success!')
          this.$router.push('/login')
          this.loading = false
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadUsers () {
      this.loadingUsers = true
      UserService.findAll()
        .then((users) => {
          this.usernames = users.map(u => u.username)
        })
        .catch(() => {
          this.loadingUsers = false
        })
        .finally(() => {
          this.loadingUsers = false
        })
    }
  }
}
</script>
