<template>
  <div v-if="token">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
          <v-card-title>Verify E-Mail-Address</v-card-title>
          <v-card-text>
            <v-form v-model="valid1" @submit.prevent="submit">
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="email"
                    :disabled="user.email_verified || error"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="E-Mail Address *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-btn
                    v-model="user.email"
                    small
                    :disabled="user.email_verified || error"
                    color="secondary"
                    type="submit"
                    @click="resend">
                    Resend E-Mail
                  </v-btn>
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
          <v-card-title>Password Change</v-card-title>
          <v-card-text>
            <v-form v-model="valid2" @submit.prevent="submit">
              <v-row dense>
                <v-col cols="5">
                  <v-text-field
                    v-model="reset.password"
                    :disabled="error"
                    type="password"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Password *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="5">
                  <v-btn
                    small
                    color="primary"
                    :disabled="!valid2 || error"
                    type="submit"
                    @click="changePassword">
                    Change
                  </v-btn>
                </v-col>
              </v-row>
              <pre>{{ $refs.form3 }}</pre>
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
      email: null,
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
      },
      reset: {
        password: null
      },
      password2: null
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
        const res = await this.$axios.put('/api/auth', {}, this.config)
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
    async resend () {
      try {
        this.loading = true
        const res = await this.$axios.post('/api/user/token/resend', {
          email: this.email
        }, this.config)
        console.debug('resend', res.data)
        this.error = false
        this.$toast.success('Successfully sent a verification e-mail')
      } catch (err) {
        console.error('resend', err)
        this.error = true
      }
      this.loading = false
    },
    async changePassword () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/user/${this.user.id}/password`, this.reset, this.config)
        console.debug('password', res.data)
        this.error = false
        this.$toast.success('Successfully changed the password')
      } catch (err) {
        console.error('password', err)
        this.error = true
      }
      this.loading = false
    }
  }
}
</script>
