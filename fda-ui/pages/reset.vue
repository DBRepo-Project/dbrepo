<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card v-if="!error && !token">
        <v-card-title>
          Reset Password
        </v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="password"
                autocomplete="off"
                type="password"
                required
                :rules="[v => !!v || $t('Required')]"
                label="Password *" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="data.password"
                autocomplete="off"
                type="password"
                required
                :rules="[v => !!v || $t('Required'), _ => formValid || $t('Passwords not matching')]"
                label="Repeat Password *" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            id="login"
            class="mb-2 ml-2"
            :disabled="!formValid"
            color="primary"
            type="submit"
            @click="reset">
            Reset Password
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
      valid: false,
      error: false,
      password: null,
      data: {
        password: null,
        token: this.$route.query.token
      }
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    formValid () {
      if (this.password === null || this.data.password === null) {
        return false
      }
      return this.password === this.data.password
    }
  },
  mounted () {
    if (!this.$route.query.token) {
      console.error('missing token!')
      this.$toast.error('Missing token!')
      this.error = true
    }
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    login () {
      this.$router.push('/login')
    },
    signup () {
      this.$router.push('/signup')
    },
    async reset () {
      try {
        this.loading = true
        const res = await this.$axios.put('/api/user/reset', this.data)
        this.loading = false
        console.debug('reset user', res.data)
        this.$router.push('/login?password_reset')
      } catch (err) {
        this.loading = false
        if (err.response !== undefined && err.response.status !== undefined) {
          if (err.response.status === 302) {
            this.$toast.success('Password successfully reset!')
            this.$router.push('/login?password_reset')
            return
          } else if (err.response.status === 417) {
            this.$toast.error('Token is invalid!')
          }
        }
        console.error('login reset failed', err)
      }
      this.loading = false
    }
  }
}
</script>
