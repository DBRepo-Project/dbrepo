<template>
  <div>
    <v-toolbar
      :title="$t('pages.signup.name')"
      flat />
    <v-form
      ref="form"
      v-model="valid"
      @submit.prevent="submit">
      <v-card
        variant="flat"
        rounded="0">
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
                :rules="[v => !!v || $t('validation.required')]"
                :hint="$t('pages.signup.email.hint')"
                :label="$t('pages.signup.email.label')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="createAccount.username"
                autocomplete="off"
                required
                name="username"
                :rules="[v => !!v || $t('validation.required'),
                         v => /^[a-z0-9]{3,}$/.test(v) || $t('validation.user.pattern'),
                         v => !usernames.includes(v) || $t('validation.user.exists')]"
                persistent-hint
                :hint="$t('pages.signup.username.hint')"
                :label="$t('pages.signup.username.label')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="createAccount.password"
                autocomplete="off"
                required
                name="password"
                :rules="[v => !!v || $t('validation.required')]"
                type="password"
                persistent-hint
                :label="$t('pages.signup.password.label')"
                :hint="$t('pages.signup.password.hint')" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col sm="6">
              <v-text-field
                v-model="password2"
                autocomplete="off"
                required
                name="password-confirm"
                :rules="[v => !!v || $t('validation.required'), v => (!!v && v) === createAccount.password || $t('Not matching!')]"
                type="password"
                persistent-hint
                :label="$t('pages.signup.confirm.label')"
                :hint="$t('pages.signup.confirm.hint')" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-text>
          <v-btn
            id="login"
            variant="flat"
            :disabled="!valid"
            color="primary"
            type="submit"
            name="submit"
            :text="$t('pages.signup.submit.label')"
            :loading="loading"
            @click="register" />
        </v-card-text>
      </v-card>
    </v-form>
  </div>
</template>

<script>
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
  mounted () {
    this.loadUsers()
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    register () {
      this.loading = true
      const userService = useUserService()
      userService.create(this.createAccount)
        .then(() => {
          this.$toast.success(this.$t('success.signup'))
          this.$router.push('/login')
          this.loading = false
        })
        .catch((error) => {
          this.$toast.error(this.$t(error.code))
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadUsers () {
      this.loadingUsers = true
      const userService = useUserService()
      userService.findAll()
        .then((users) => {
          this.usernames = users.map(u => u.username)
        })
        .catch((error) => {
          this.$toast.error(this.$t(error.code))
          this.loadingUsers = false
        })
        .finally(() => {
          this.loadingUsers = false
        })
    }
  }
}
</script>
