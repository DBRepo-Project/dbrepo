<template>
  <div>
    <v-toolbar
      v-if="!user"
      variant="flat"
      :title="$t('pages.login.name')">
    </v-toolbar>
    <v-card
      rounded="0"
      variant="flat">
      <v-card-text>
        <v-form
          v-if="!user"
          ref="form"
          v-model="valid"
          @submit.prevent="submit">
          <v-row
            dense>
            <v-col
              md="8">
              <v-text-field
                v-model="username"
                autocomplete="off"
                autofocus
                required
                name="username"
                persistent-hint
                :rules="[v => !!v || $t('validation.required')]"
                :label="$t('pages.login.username.label')"
                :hint="$t('pages.login.username.hint')"/>
            </v-col>
          </v-row>
          <v-row
            dense>
            <v-col
              md="8">
              <v-text-field
                v-model="password"
                autocomplete="off"
                type="password"
                required
                name="password"
                persistent-hint
                :rules="[v => !!v || $t('validation.required')]"
                :label="$t('pages.login.password.label')"
                :hint="$t('pages.login.password.hint')"/>
            </v-col>
          </v-row>
          <v-row>
            <v-col
              md="8">
              <v-btn
                id="login"
                class="mb-2"
                :disabled="!valid"
                color="primary"
                variant="flat"
                type="submit"
                name="submit"
                :loading="loading"
                :text="$t('pages.login.submit.label')"
                @click="login"/>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer/>
        <v-btn
          v-for="(link, i) in loginLinks"
          :key="`li-${i}`"
          variant="plain"
          size="small"
          :text="link.text"
          :href="link.href"/>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import {useUserStore} from '@/stores/user'
import {localizedMessage} from '@/utils'

export default {
  data() {
    return {
      loading: false,
      valid: false,
      username: null,
      password: null,
      userStore: useUserStore()
    }
  },
  computed: {
    user() {
      return this.userStore.getUser
    },
    loginLinks() {
      if (!this.$config.public.links) {
        return []
      }
      return Object.keys(this.$config.public.links).map(key => {
        return this.$config.public.links[key]
      })
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate()
    },
    login() {
      this.loading = true
      const authenticationService = useAuthenticationService()
      authenticationService.authenticatePlain(this.username, this.password)
        .then((data) => {
          const userService = useUserService()
          const userId = userService.tokenToUserId(data.access_token)
          userService.findOne(userId)
            .then((user) => {
              switch (user.attributes.theme) {
                case 'dark':
                  this.$vuetify.theme.global.name = 'tuwThemeDark'
                  break
                case 'light':
                  this.$vuetify.theme.global.name = 'tuwThemeLight'
                  break
                case 'light-contrast':
                  this.$vuetify.theme.global.name = 'tuwThemeLightContrast'
                  break
                case 'dark-contrast':
                  this.$vuetify.theme.global.name = 'tuwThemeDarkContrast'
                  break
              }
              this.userStore.setUser(user)
              this.$router.push('/database')
            })
        })
        .catch((error) => {
          console.error('Failed to login', error)
          const {status} = error.response
          if (status === 401) {
            this.$toast.error(this.$t('error.user.credentials'))
          } else {
            this.$toast.error(localizedMessage(this.$t, error, null))
          }
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
