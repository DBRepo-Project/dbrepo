<template>
  <div>
    <UserToolbar />
    <v-window v-model="tab">
      <v-window-item>
        <v-form v-model="valid1" @submit.prevent="submit">
          <v-card
            :title="$t('pages.user.subpages.info.title')"
            :subtitle="$t('pages.user.subpages.info.subtitle')"
            rounded="0"
            variant="flat">
            <v-card-text>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.id"
                    disabled
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.info.id.label')" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.username"
                    disabled
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.info.username.label')"  />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="6">
                  <v-select
                    v-model="model.theme"
                    :items="themes"
                    item-title="name"
                    item-value="value"
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.theme.label')" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col cols="6">
                  <v-select
                    v-model="model.language"
                    :items="languages"
                    item-title="name"
                    item-value="value"
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.language.label')" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.orcid"
                    :disabled="!canModifyInformation"
                    maxlength="37"
                    clearable
                    :loading="orcidLoading"
                    persistent-hint
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.info.orcid.label')"
                    :hint="$t('pages.user.subpages.info.orcid.hint')"
                    @focusout="retrieve" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.firstname"
                    :disabled="!canModifyInformation"
                    clearable
                    persistent-hint
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.info.firstname.label')"
                    :hint="$t('pages.user.subpages.info.firstname.hint')" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.lastname"
                    :disabled="!canModifyInformation"
                    clearable
                    persistent-hint
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.info.lastname.label')"
                    :hint="$t('pages.user.subpages.info.lastname.hint')" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="6">
                  <v-text-field
                    v-model="model.affiliation"
                    :disabled="!canModifyInformation"
                    clearable
                    persistent-hint
                    :variant="inputVariant"
                    :label="$t('pages.user.subpages.info.affiliation.label')"
                    :hint="$t('pages.user.subpages.info.affiliation.hint')" />
                </v-col>
              </v-row>
              <v-row>
                <v-col>
                  <v-btn
                    size="small"
                    :disabled="!canModifyInformation"
                    variant="flat"
                    color="secondary"
                    :loading="loadingUpdate"
                    :text="$t('pages.user.subpages.info.submit.text')"
                    @click="updateInfo" />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>
        </v-form>
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
      error: false,
      loadingUpdate: false,
      loadingTheme: false,
      theme: null,
      orcidLoading: false,
      model: {
        id: null,
        username: null,
        firstname: null,
        lastname: null,
        theme: null,
        language: null
      },
      themes: [
        { name: this.$t('pages.user.subpages.theme.light'), value: 'light' },
        { name: this.$t('pages.user.subpages.theme.light-contrast'), value: 'light-contrast' },
        { name: this.$t('pages.user.subpages.theme.dark'), value: 'dark' },
        { name: this.$t('pages.user.subpages.theme.dark-contrast'), value: 'dark-contrast' },
      ],
      languages: [
        { name: this.$t('pages.user.subpages.language.en'), value: 'en' },
        { name: this.$t('pages.user.subpages.language.de'), value: 'de' }
      ],
      items: [
        {
          title: this.$t('navigation.user'),
          to: '/user'
        },
        {
          title: this.$t('toolbars.user.info'),
          to: `/user/info`,
          disabled: true
        }
      ],
      userStore: useUserStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    roles () {
      return this.userStore.getRoles
    },
    locale () {
      return this.userStore.getLocale
    },
    canModifyTheme () {
      return this.roles.includes('modify-user-theme')
    },
    canModifyInformation () {
      return this.roles.includes('modify-user-information')
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
    this.init()
  },
  methods: {
    submit () {
    },
    updateInfo () {
      this.loadingUpdate = true
      const payload = {
        firstname: this.model.firstname,
        lastname: this.model.lastname,
        orcid: this.model.orcid,
        affiliation: this.model.affiliation,
        theme: this.model.theme,
        language: this.model.language,
      }
      const userService = useUserService()
      userService.update(this.user.id, payload)
        .then((user) => {
          console.info('Updated user information')
          const toast = useToastInstance()
          toast.success(this.$t('success.user.info'))
          this.userStore.setUser(user)
          /* language */
          this.userStore.setLocale(this.model.language)
          this.$i18n.locale = this.locale
          /* theme */
          switch (this.model.theme) {
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
        })
        .catch(() => {
          this.loadingUpdate = false
        })
        .finally(() => {
          this.loadingUpdate = false
        })
    },
    init () {
      if (!this.user) {
        return
      }
      this.model = {
        id: this.user.id,
        username: this.user.username,
        firstname: this.user.given_name,
        lastname: this.user.family_name,
        orcid: this.user.attributes.orcid,
        affiliation: this.user.attributes.affiliation,
        theme: this.user.attributes.theme,
        language: this.user.attributes.language
      }
    },
    retrieve () {
      if (!this.model.orcid) {
        return
      }
      this.orcidLoading = true
      const identifierService = useIdentifierService()
      identifierService.suggest(this.model.orcid)
        .then((metadata) => {
          this.model.firstname = metadata?.given_names
          this.model.lastname = metadata?.family_name
            if (metadata.affiliations.length > 0) {
              this.model.affiliation = metadata.affiliations[0].organization_name
          }
          this.orcidLoading = false
        })
        .catch(() => {
          this.orcidLoading = false
        })
        .finally(() => {
          this.orcidLoading = false
        })
    }
  }
}
</script>
