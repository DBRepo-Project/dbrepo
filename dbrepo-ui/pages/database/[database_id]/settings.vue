<template>
  <div
    v-if="canViewSettings">
    <DatabaseToolbar
      ref="toolbar" />
    <v-window
      v-model="tab">
      <v-window-item>
        <v-card
          v-if="canModifyImage"
          variant="flat"
          rounded="0"
          :title="$t('pages.database.subpages.settings.title')"
          :subtitle="$t('pages.database.subpages.settings.subtitle')">
          <v-card-text>
            <v-form
              ref="form"
              v-model="validUpload"
              @submit.prevent="submit">
              <v-row
                v-if="previewImage"
                dense>
                <v-col
                  lg="8">
                  <v-alert
                    v-if="file"
                    border="start"
                    color="warning">
                    {{ $t('pages.database.image.warn') }}
                  </v-alert>
                  <v-img
                    class="mt-2"
                    :src="previewImage"
                    :alt="$t('pages.database.image.alt')"
                    :title="$t('pages.database.image.alt')"
                    :max-width="maxWidth"
                    :max-height="maxHeight" />
                  <v-btn
                    v-if="database.preview_image"
                    size="small"
                    variant="flat"
                    color="error"
                    class="ml-2 mt-4"
                    :text="$t('pages.database.subpages.settings.image-remove.text')"
                    :loading="loadingDeleteImage"
                    @click="removeDatabaseImage" />
                </v-col>
              </v-row>
              <v-row
                dense>
                <v-col
                  lg="8">
                  <v-file-input
                    v-model="file"
                    accept="image/*"
                    :hint="$t('pages.database.subpages.settings.image.hint')"
                    persistent-hint
                    clearable
                    :variant="inputVariant"
                    variant="underlined"
                    :loading="loadingUpload"
                    :error-messages="uploadErrorMessages"
                    :show-size="1000"
                    counter
                    :label="$t('pages.database.subpages.settings.image.label')"
                    @update:modelValue="uploadFile">
                    <template
                      v-if="uploadProgress"
                      v-slot:append>
                      <span>{{ uploadProgress }}%</span>
                    </template>
                  </v-file-input>
                </v-col>
              </v-row>
              <v-row
                dense>
                <v-col>
                  <v-btn
                    size="small"
                    variant="flat"
                    color="secondary"
                    :disabled="!modifyImage.key"
                    class="mt-4"
                    :text="$t('pages.database.subpages.settings.submit.text')"
                    :loading="loadingImage"
                    @click="updateDatabaseImage" />
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          variant="flat"
          rounded="0"
          :title="$t('pages.database.subpages.access.title')"
          :subtitle="$t('pages.database.subpages.access.subtitle')" >
          <v-data-table
            :headers="headers"
            :items="accesses"
            :items-per-page="10">
            <template v-slot:item.action="{ item }">
              <v-btn
                v-if="item && item.user && item.user.username !== cacheUser.preferred_username"
                size="x-small"
                variant="flat"
                color="warning"
                :disabled="!canModifyAccess"
                :text="$t('navigation.modify')"
                @click="modifyAccess(item)" />
            </template>
          </v-data-table>
          <v-card-text>
            <v-btn
              size="small"
              variant="flat"
              :disabled="!canCreateAccess"
              color="warning"
              :text="$t('navigation.create')"
              @click="giveAccess" />
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          v-if="canModifyVisibility"
          variant="flat"
          rounded="0"
          :title="$t('pages.database.subpages.settings.visibility.title')"
          :subtitle="$t('pages.database.subpages.settings.visibility.subtitle')">
          <v-card-text>
            <v-row
              dense>
              <v-col
                lg="4">
                <v-select
                  v-model="modifyVisibility.is_public"
                  :items="dataOptions"
                  persistent-hint
                  :variant="inputVariant"
                  required
                  :rules="[
                    v => v !== null || $t('validation.required')
                  ]"
                  :label="$t('pages.database.resource.data.label')"
                  :hint="$t('pages.database.resource.data.hint', { resource: 'database' })" />
              </v-col>
              <v-col
                lg="4">
                <v-select
                  v-model="modifyVisibility.is_schema_public"
                  :items="schemaOptions"
                  persistent-hint
                  :variant="inputVariant"
                  required
                  :rules="[
                    v => v !== null || $t('validation.required')
                  ]"
                  :label="$t('pages.database.resource.schema.label')"
                  :hint="$t('pages.database.resource.schema.hint', { resource: 'database', schema: 'tables, views, subsets' })" />
              </v-col>
            </v-row>
            <v-row
              v-if="isDashboardDisabled">
              <v-col
                lg="8">
                <v-alert
                  border="start"
                  color="warning">
                  {{ $t('pages.database.dashboard.visibility.warn') }}
                </v-alert>
              </v-col>
            </v-row>
            <v-row
              dense>
              <v-col
                lg="4">
                <v-select
                  v-model="modifyVisibility.is_dashboard_enabled"
                  :items="dashboardOptions"
                  persistent-hint
                  :variant="inputVariant"
                  required
                  :rules="[
                    v => v !== null || $t('validation.required')
                  ]"
                  :label="$t('pages.database.dashboard.visibility.label')"
                  :hint="$t('pages.database.dashboard.visibility.hint')" />
              </v-col>
            </v-row>
            <v-row>
              <v-col>
                <v-btn
                  size="small"
                  variant="flat"
                  :color="isSameVisibility ? null : 'warning'"
                  :disabled="isSameVisibility"
                  :text="$t('pages.database.subpages.settings.visibility.submit.text')"
                  @click="updateDatabaseVisibility" />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          v-if="canModifyOwnership"
          :title="$t('pages.database.subpages.settings.ownership.title')"
          :subtitle="$t('pages.database.subpages.settings.ownership.subtitle')"
          variant="flat"
          rounded="0">
          <v-card-text>
            <v-row>
              <v-col
                lg="8">
                <v-select
                  v-model="modifyOwner.username"
                  :items="users"
                  item-title="username"
                  item-value="username"
                  persistent-hint
                  :variant="inputVariant"
                  :hint="$t('pages.database.subpages.settings.ownership.hint')"
                  :label="$t('pages.database.subpages.settings.ownership.label')"
                  name="owner" />
              </v-col>
            </v-row>
            <v-row>
              <v-col>
                <v-btn
                  size="small"
                  variant="flat"
                  :color="isSameOwner ? null : 'warning'"
                  :disabled="isSameOwner"
                  :text="$t('pages.database.subpages.settings.ownership.submit.text')"
                  @click="updateDatabaseOwner" />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          v-if="canUpdateScheme"
          :title="$t('pages.database.subpages.settings.scheme.title')"
          :subtitle="$t('pages.database.subpages.settings.scheme.subtitle')"
          variant="flat"
          rounded="0">
          <v-card-text>
            <v-row>
              <v-col>
                <v-btn
                  size="small"
                  variant="flat"
                  color="tertiary"
                  :loading="loadingSchema"
                  :text="$t('pages.database.subpages.settings.scheme.submit.text')"
                  @click="refreshSchema" />
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-window-item>
      <v-dialog
        v-model="editAccessDialog"
        max-width="640">
        <EditAccess :user-id="username" :access-type="accessType" @close-dialog="closeDialog" />
      </v-dialog>
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import EditAccess from '@/components/dialogs/EditAccess.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    DatabaseToolbar,
    EditAccess
  },
  data () {
    return {
      dialogDelete: false,
      confirm: null,
      username: null,
      accessType: null,
      users: [],
      loading: false,
      loadingUpload: false,
      loadingImage: false,
      loadingSchema: false,
      validUpload: false,
      loadingDeleteImage: false,
      file: null,
      loadingUsers: false,
      editAccessDialog: false,
      editVisibilityDialog: false,
      modifyVisibility: {
        is_public: null,
        is_schema_public: null,
        is_dashboard_enabled: null,
      },
      modifyOwner: {
        username: null
      },
      modifyImage: {
        key: null
      },
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
      dashboardOptions: [
        { title: this.$t('navigation.enabled'), value: true },
        { title: this.$t('navigation.disabled'), value: false },
      ],
      headers: [
        {
          title: this.$t('pages.user.qualified-name.label'),
          value: 'qualified_name',
          sortable: false
        },
        {
          title: this.$t('pages.database.subpages.access.type.label'),
          value: 'type',
          sortable: false
        },
        {
          title: this.$t('pages.database.subpages.access.action'),
          value: 'action',
          sortable: false
        }
      ],
      accesses: [],
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.settings'),
          to: `/database/${this.$route.params.database_id}/settings`,
          disabled: true
        }
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    tab () {
      return 0
    },
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.cacheStore.getAccess
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    uploadProgress () {
      return this.cacheStore.getUploadProgress
    },
    isSameOwner () {
      if (!this.modifyOwner || !this.cacheUser) {
        return false
      }
      return this.modifyOwner.username === this.cacheUser.preferred_username
    },
    isSameVisibility () {
      if (!this.modifyVisibility || !this.database) {
        return false
      }
      return this.modifyVisibility.is_public === this.database.is_public && this.modifyVisibility.is_schema_public === this.database.is_schema_public && this.modifyVisibility.is_dashboard_enabled === this.database.is_dashboard_enabled
    },
    canModifyVisibility () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('modify-database-visibility')
    },
    canModifyOwnership () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('modify-database-owner')
    },
    canUpdateScheme () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('find-database')
    },
    canModifyAccess () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('update-database-access')
    },
    canCreateAccess () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('create-database-access')
    },
    canModifyImage () {
      if (!this.roles) {
        return false
      }
      return this.roles.includes('modify-database-image')
    },
    canViewSettings () {
      if (!this.database || !this.cacheUser) {
        return false
      }
      return this.database.owner.username === this.cacheUser.preferred_username
    },
    previewImage () {
      if (this.file) {
        return URL.createObjectURL(this.file)
      }
      if (!this.database) {
        return null
      }
      return this.database.preview_image
    },
    maxWidth () {
      return this.$config.public.database.image.width
    },
    maxHeight () {
      return this.$config.public.database.image.height
    },
    isDashboardDisabled () {
      if (!this.database) {
        return false
      }
      return this.database.is_dashboard_enabled && !this.modifyVisibility.is_dashboard_enabled
    },
    uploadErrorMessages () {
      if (!this.file || this.file.size < 1_000_000) {
        return []
      }
      return [this.$t('validation.image.size')]
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
  watch: {
    database (val) {
      if (!val) {
        return
      }
      this.modifyVisibility.is_public = this.database.is_public
      this.modifyOwner.username = this.database.owner.username
    }
  },
  mounted () {
    if (this.users.length === 0) {
      this.loadUsers()
    }
    if (!this.database) {
      return
    }
    this.modifyVisibility.is_public = this.database.is_public
    this.modifyVisibility.is_schema_public = this.database.is_schema_public
    this.modifyVisibility.is_dashboard_enabled = this.database.is_dashboard_enabled
    this.modifyOwner.username = this.database.owner.username
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    closeDialog () {
      this.editAccessDialog = false
      this.cacheStore.reloadDatabase()
    },
    updateDatabaseVisibility () {
      this.loading = true
      const databaseService = useDatabaseService()
      databaseService.updateVisibility(this.$route.params.database_id, this.modifyVisibility)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.database.visibility'))
          this.cacheStore.reloadDatabase()
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    },
    uploadFile () {
      console.debug('upload file', this.file)
      if (this.file.size > 1_000_000) {
        const toast = useToastInstance()
        toast.error(this.$t('error.image.size'))
      }
      this.loadingUpload = true
      const uploadService = useUploadService()
      uploadService.create(this.file)
        .then((s3_key) => {
          console.debug('uploaded image', s3_key)
          const cacheStore = useCacheStore()
          cacheStore.setUploadProgress(null)
          const toast = useToastInstance()
          toast.success(this.$t('success.database.upload'))
          this.modifyImage.key = s3_key
          this.loadingUpload = false
        })
        .catch((error) => {
          console.error('Failed to upload dataset', error)
          const toast = useToastInstance()
          toast.error(this.$t('error.upload.dataset'))
          this.loading = false
        })
        .finally(() => {
          this.loadingUpload = false
        })
    },
    updateDatabaseImage () {
      this.loadingImage = true
      const databaseService = useDatabaseService()
      databaseService.updateImage(this.$route.params.database_id, this.modifyImage)
        .then(() => {
          this.cacheStore.reloadDatabase()
          const toast = useToastInstance()
          toast.success(this.$t('success.database.image.update'))
          this.modifyImage.key = null
          this.loadingImage = false
          this.file = null;
        })
        .catch(({code, message}) => {
          this.loadingImage = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingImage = false
        })
    },
    removeDatabaseImage () {
      this.loadingDeleteImage = true
      const databaseService = useDatabaseService()
      databaseService.updateImage(this.$route.params.database_id, { key: null })
        .then(() => {
          this.cacheStore.reloadDatabase()
          const toast = useToastInstance()
          toast.success(this.$t('success.database.image.remove'))
          this.loadingDeleteImage = false
        })
        .catch(({code, message}) => {
          this.loadingDeleteImage = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingDeleteImage = false
        })
    },
    updateDatabaseOwner () {
      this.loading = true
      const databaseService = useDatabaseService()
      databaseService.updateOwner(this.$route.params.database_id, { username: this.modifyOwner.username })
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.database.transfer'))
          this.$router.push(`/database/${this.$route.params.database_id}/info`)
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    },
    refreshSchema () {
      this.loadingSchema = true
      const databaseService = useDatabaseService()
      databaseService.refreshTablesMetadata(this.$route.params.database_id)
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.schema.tables'))
          databaseService.refreshViewsMetadata(this.$route.params.database_id)
            .then(() => {
              const toast = useToastInstance()
              toast.success(this.$t('success.schema.views'))
              this.cacheStore.reloadDatabase()
              this.loadingSchema = false
            })
            .catch(({code, message}) => {
              this.loadingSchema = false
              const toast = useToastInstance()
              if (typeof code !== 'string') {
                toast.error(message)
                return
              }
              toast.error(this.$t(code))
            })
        })
        .catch(({code, message}) => {
          this.loadingSchema = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
    },
    giveAccess () {
      this.username = null
      this.accessType = null
      this.editAccessDialog = true
    },
    modifyAccess (item) {
      this.username = item.user.username
      this.accessType = item.type
      this.editAccessDialog = true
    },
    loadUsers () {
      this.loadingUsers = true
      const userService = useUserService()
      userService.findAll()
        .then((users) => {
          this.users = users
          this.loadingUsers = false
        })
        .catch(({code, message}) => {
          this.loadingUsers = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            toast.error(message)
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingUsers = false
        })
    }
  }
}
</script>
