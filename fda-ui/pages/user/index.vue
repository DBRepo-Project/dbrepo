<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
    <v-toolbar flat>
      <v-toolbar-title>
        <v-skeleton-loader v-if="loading || error" type="text" width="200" />
        <span v-if="!loading && !error">
          {{ name }} <sup v-if="user.email_verified">
            <v-icon color="primary" title="E-Mail verified" small>mdi-check-decagram</v-icon>
          </sup>
        </span>
      </v-toolbar-title>
    </v-toolbar>
    <v-card flat>
      <v-card-title>Verify E-Mail-Address</v-card-title>
      <v-card-text>
        <v-form v-model="valid1" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="5">
              <v-text-field
                v-model="user.email"
                :disabled="user.email_verified"
                :rules="[v => !!v || $t('Required')]"
                required
                label="E-Mail Address *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-btn
                :disabled="user.email_verified"
                color="secondary"
                type="submit"
                @click="resend">
                Resend E-Mail
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-divider />
      <v-card-title>User Information</v-card-title>
      <v-card-text>
        <v-form v-model="valid2" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="2">
              <v-text-field
                v-model="user.id"
                disabled
                label="ID" />
            </v-col>
            <v-col cols="3">
              <v-text-field
                v-model="user.username"
                disabled
                label="Username" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-text-field
                v-model="user.titles_before"
                hint="e.g. Prof."
                label="Titles Before" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-text-field
                v-model="user.firstname"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Firstname *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-text-field
                v-model="user.lastname"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Lastname *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-text-field
                v-model="user.titles_after"
                hint="e.g. BSc"
                label="Titles After" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-btn
                color="primary"
                :disabled="!valid2"
                type="submit"
                @click="updateInfo">
                Update
              </v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-divider />
      <v-card-title>Password Change</v-card-title>
      <v-card-text>
        <v-form v-model="valid3" @submit.prevent="submit">
          <v-row dense>
            <v-col cols="5">
              <v-text-field
                v-model="reset.password"
                type="password"
                :rules="[v => !!v || $t('Required')]"
                required
                label="Password *" />
            </v-col>
          </v-row>
          <v-row dense>
            <v-col cols="5">
              <v-btn
                color="primary"
                :disabled="!valid3"
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
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
export default {
  data () {
    return {
      valid1: true,
      valid2: true,
      valid3: true,
      valid4: true,
      user: {
        id: null,
        email: null,
        username: null,
        lastname: null,
        firstname: null,
        titles_after: null,
        titles_before: null,
        email_verified: false
      },
      reset: {
        password: null
      },
      password2: null,
      items: [
        { text: 'Databases', to: '/container', activeClass: '' }
      ],
      loading: false,
      error: false
    }
  },
  computed: {
    name () {
      if (this.user.firstname && this.user.lastname) {
        let name = ''
        if (this.user.titles_before) {
          name += this.user.titles_before + ' '
        }
        name += this.user.firstname + ' ' + this.user.lastname
        if (this.user.titles_after) {
          name += ' ' + this.user.titles_after
        }
        return name
      }
      return this.user.username
    },
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
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
      try {
        this.loading = true
        const res = await this.$axios.put('/api/auth/', {}, this.config)
        this.user = res.data
        console.debug('user', this.user)
        this.error = false
      } catch (err) {
        console.error('user', err)
        this.error = true
      }
      this.loading = false
    },
    async resend () {
      try {
        this.loading = true
        const res = await this.$axios.post('/api/user/token/resend', {
          email: this.user.email
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
        const res = await this.$axios.put('/api/user/' + this.user.id + '/password', this.reset, this.config)
        console.debug('password', res.data)
        this.error = false
        this.$toast.success('Successfully changed the password')
      } catch (err) {
        console.error('password', err)
        this.error = true
      }
      this.loading = false
    },
    async updateInfo () {
      try {
        this.loading = true
        const res = await this.$axios.put('/api/user/' + this.user.id, {
          titles_before: this.user.titles_before,
          titles_after: this.user.titles_after,
          firstname: this.user.firstname,
          lastname: this.user.lastname
        }, this.config)
        console.debug('update', res.data)
        this.error = false
        this.$toast.success('Successfully updated user info')
      } catch (err) {
        console.error('update', err)
        this.error = true
      }
      this.loading = false
    },
    setToken () {
      this.user.has_invenio_token = false
      this.api.invenio_token = ''
    }
  }
}
</script>

<style>
</style>
