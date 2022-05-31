<template>
  <div>
    <v-form ref="form" v-model="valid" @submit.prevent="submit">
      <v-card v-if="!token">
        <v-progress-linear v-if="loading" color="primary" :indeterminate="!error" />
        <v-card-title>
          Forgot Login
        </v-card-title>
        <v-card-text>
          <v-alert
            border="left"
            color="info">
            If you need an account, <a @click="signup">create one</a>. You can also <a @click="login">login</a> instead.
          </v-alert>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="data.username"
                autocomplete="off"
                autofocus
                label="Username" />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model="data.email"
                autocomplete="off"
                type="email"
                label="E-Mail Address" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-btn
            id="login"
            class="mb-2 ml-2"
            :disabled="!valid || !formValid"
            color="primary"
            type="submit"
            @click="forgot">
            Send Information
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
      valid: false,
      error: false,
      data: {
        username: null,
        email: null
      }
    }
  },
  computed: {
    token () {
      return this.$store.state.token
    },
    formValid () {
      if (this.data.username === null && this.data.email === null) {
        return false
      }
      if (this.data.username === null && this.data.email !== null) {
        return this.data.email.length > 0
      }
      if (this.data.username !== null && this.data.email === null) {
        return this.data.username.length > 0
      }
      if (this.data.username !== null && this.data.email !== null) {
        return this.data.username.length > 0 || this.data.email.length > 0
      }
      return false
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
    async forgot () {
      try {
        this.loading = true
        const res = await this.$axios.put('/api/user', this.data)
        console.debug('reset user', res.data)
        this.$toast.success('If a user with this information exists, we sent you a mail!')
      } catch (err) {
        console.error('login reset failed', err)
      }
      this.loading = false
    }
  }
}
</script>
