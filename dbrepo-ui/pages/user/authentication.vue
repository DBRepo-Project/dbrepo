<template>
  <div>
    <UserToolbar />
    <v-window v-model="tab">
      <v-window-item>
        <v-card
          :title="$t('pages.settings.subpages.authentication.title')"
          :subtitle="$t('pages.settings.subpages.authentication.subtitle')"
          variant="flat"
          rounded="0">
          <v-card-text>
            <v-form
              v-model="valid2"
              @submit.prevent="submit">
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="password"
                    type="password"
                    :rules="[v => !!v || $t('validation.required')]"
                    required
                    :variant="inputVariant"
                    persistent-hint
                    :label="$t('pages.settings.subpages.authentication.password.label')"
                    :hint="$t('pages.settings.subpages.authentication.password.hint')" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="password2"
                    type="password"
                    :rules="[v => !!v || $t('validation.required'), v => (!!v && v) === password || $t('Not matching!')]"
                    required
                    :variant="inputVariant"
                    persistent-hint
                    :label="$t('pages.settings.subpages.authentication.confirm.label')"
                    :hint="$t('pages.settings.subpages.authentication.confirm.hint')" />
                </v-col>
              </v-row>
              <v-row>
                <v-col md="6">
                  <v-btn
                    size="small"
                    color="secondary"
                    :loading="loadingUpdate"
                    :disabled="!valid2"
                    variant="flat"
                    type="submit"
                    :text="$t('pages.settings.subpages.authentication.submit.text')"
                    @click="changePassword" />
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import UserToolbar from '@/components/user/UserToolbar.vue'
import { useUserStore } from '@/stores/user.js'

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
      items: [
        {
          title: this.$t('navigation.user'),
          to: '/user'
        },
        {
          title: this.$t('toolbars.user.authentication'),
          to: `/user/authentication`,
          disabled: true
        }
      ],
      email: null,
      password: null,
      password2: null,
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  mounted () {
  },
  methods: {
    submit () {
    },
    changePassword () {
      this.loadingUpdate = true
      const userService = useUserService()
      userService.updatePassword(this.user.id, {'password': this.password})
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.user.password'))
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
