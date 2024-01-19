<template>
  <div v-if="token">
    <UserToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat tile>
          <v-card-title>Password Change</v-card-title>
          <v-card-text>
            <v-form v-model="valid2" @submit.prevent="submit">
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="password"
                    type="password"
                    :rules="[v => !!v || $t('Required')]"
                    required
                    label="Password *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="password2"
                    type="password"
                    :rules="[v => !!v || $t('Required'), v => (!!v && v) === password || $t('Not matching!')]"
                    required
                    label="Repeat Password *" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-btn
                    small
                    color="primary"
                    :loading="loadingUpdate"
                    :disabled="!valid2"
                    type="submit"
                    @click="changePassword">
                    Change
                  </v-btn>
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
  </div>
</template>

<script>
import UserToolbar from '@/components/UserToolbar'
import UserService from '@/api/user.service'

export default {
  components: {
    UserToolbar
  },
  data () {
    return {
      tab: 0,
      valid1: false,
      valid2: false,
      loadingUpdate: false,
      email: null,
      password: null,
      password2: null
    }
  },
  computed: {
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
  },
  methods: {
    submit () {
    },
    changePassword () {
      this.loadingUpdate = true
      UserService.updatePassword(this.user.id, this.password)
        .then(() => {
          this.$toast.success('Successfully changed the password')
          this.loadingUpdate = false
        })
        .catch(() => {
          this.loadingUpdate = false
        })
        .finally(() => {
          this.loadingUpdate = false
        })
    }
  }
}
</script>
