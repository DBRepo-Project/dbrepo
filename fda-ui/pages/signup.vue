<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card>
        <v-card-title>
          Create Account
        </v-card-title>
        <v-card-text>
          <v-alert
            border="left"
            color="info">
            Before you can use the repository sandbox, you will need to <i>confirm</i> your email address, make sure to check your spam folder.
          </v-alert>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="createAccount.email"
                type="email"
                autocomplete="off"
                autofocus
                required
                :rules="[v => !!v || $t('Required')]"
                hint="e.g. max.mustermann@work.com"
                label="Work E-Mail Address *" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="createAccount.username"
                autocomplete="off"
                required
                :rules="[v => !!v || $t('Required'),
                         v => /^[a-z0-9]{3,}$/.test(v) || $t('Only lowercase letters, min. 3 length')]"
                hint="e.g. mmustermann"
                label="Username *" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="createAccount.password"
                autocomplete="off"
                required
                :rules="[v => !!v || $t('Required')]"
                type="password"
                label="Password *" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-checkbox
                v-model="consent"
                required
                :rules="[v => !!v || $t('Required')]"
                label="I understand the warning and do not use production data" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-checkbox
                v-model="privacy"
                required
                :rules="[v => !!v || $t('Required')]"
                label="I have read and accept the privacy statement" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            class="mb-2 ml-2">
            Cancel
          </v-btn>
          <v-btn
            id="login"
            class="mb-2"
            :disabled="!valid"
            color="primary"
            type="submit"
            :loading="loading"
            @click="register">
            Submit
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
export default {
  data () {
    return {
      loading: false,
      error: false, // XXX: `error` is never changed
      valid: false,
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
  beforeMount () {
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    async register () {
      const url = '/api/user'
      try {
        this.loading = true
        const res = await this.$axios.post(url, this.createAccount)
        console.debug('create user', res.data)
        this.$toast.success('Success. Check your inbox!')
        this.$router.push('/login')
      } catch (err) {
        if (err.response !== undefined && err.response.status !== undefined) {
          if (err.response.status === 417) {
            this.$toast.error('This e-mail address is taken.')
            console.error('email taken', err)
            this.loading = false
            return
          }
          if (err.response.status === 409) {
            this.$toast.error('This username is taken.')
            console.error('username taken', err)
            this.loading = false
            return
          }
          if (err.response.status === 428) {
            this.$toast.warning('Account was created but the server failed to send a mail.')
            console.warn('email sending failed', err)
            this.loading = false
            return
          }
        }
        console.error('create user failed', err)
        this.$toast.error('Failed to create user')
      }
      this.loading = false
    }
  }
}
</script>
