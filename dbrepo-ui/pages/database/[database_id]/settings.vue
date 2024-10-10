<template>
  <div>
    <DatabaseToolbar
      ref="toolbar" />
    <v-window
      v-if="user"
      v-model="tab">
      <v-window-item>
        <v-card
          v-if="isOwner && canModifyImage"
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
                v-if="databaseImage"
                dense>
                <v-col md="8">
                  <v-img
                    :src="databaseImage"
                    :alt="$t('pages.database.image.alt')"
                    :max-width="maxWidth"
                    :max-height="maxHeight" />
                </v-col>
              </v-row>
              <v-row dense>
                <v-col md="8">
                  <v-file-input
                    v-model="file"
                    accept="image/*"
                    :hint="$t('pages.database.subpages.settings.image.hint')"
                    persistent-hint
                    clearable
                    :variant="inputVariant"
                    variant="underlined"
                    :loading="loadingUpload"
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
                  <v-btn
                    v-if="database.image"
                    size="small"
                    variant="flat"
                    color="error"
                    class="ml-2 mt-4"
                    :text="$t('pages.database.subpages.settings.image-remove.text')"
                    :loading="loadingDeleteImage"
                    @click="removeDatabaseImage" />
                </v-col>
              </v-row>
            </v-form>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card
          v-if="isOwner"
          variant="flat"
          rounded="0"
          :title="$t('pages.database.subpages.access.title')"
          :subtitle="$t('pages.database.subpages.access.subtitle')" >
          <v-data-table
            :headers="headers"
            :items="database.accesses"
            :items-per-page="10">
            <template v-slot:item.qualified_name="{ item }">
              <span
                v-if="item && item.user">
                {{ item.user.qualified_name }}
              </span>
            </template>
            <template v-slot:item.action="{ item }">
              <v-btn
                v-if="item && item.user && item.user.username !== user.username"
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
            <v-row>
              <v-col md="8">
                <v-select
                  v-model="modifyVisibility.is_public"
                  :items="visibility"
                  :variant="inputVariant"
                  :label="$t('pages.database.subpages.settings.visibility.visibility.label')"
                  :hint="$t('pages.database.subpages.settings.visibility.visibility.hint')"
                  persistent-hint
                  name="visibility" />
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
              <v-col md="8">
                <v-select
                  v-model="modifyOwner.id"
                  :items="users"
                  item-title="username"
                  item-value="id"
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
        <EditAccess :user-id="userId" :access-type="accessType" @close-dialog="closeDialog" />
      </v-dialog>
    </v-window>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import EditAccess from '@/components/dialogs/EditAccess.vue'
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    DatabaseToolbar,
    EditAccess
  },
  data () {
    return {
      dialogDelete: false,
      confirm: null,
      userId: null,
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
        is_public: null
      },
      modifyOwner: {
        id: null
      },
      modifyImage: {
        key: null
      },
      visibility: [
        {
          title: this.$t('toolbars.database.public'),
          value: true
        },
        {
          title: this.$t('toolbars.database.private'),
          value: false
        }
      ],
      headers: [
        {
          title: this.$t('pages.user.qualified-name.label'),
          value: 'qualified_name',
          sortable: false
        },
        {
          title: this.$t('pages.database.subpages.access.title'),
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
      userStore: useUserStore(),
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
      return this.userStore.getAccess
    },
    token () {
      return this.userStore.getToken
    },
    roles () {
      return this.userStore.getRoles
    },
    user () {
      return this.userStore.getUser
    },
    uploadProgress () {
      return this.cacheStore.getUploadProgress
    },
    isOwner () {
      if (!this.database || !this.user) {
        return false
      }
      if (this.database.owner.id === null || this.user.id === null) {
        return false
      }
      return this.database.owner.id === this.user.id
    },
    isSameOwner () {
      if (!this.modifyOwner || !this.user) {
        return false
      }
      return this.modifyOwner.id === this.user.id
    },
    isSameVisibility () {
      if (!this.modifyVisibility || !this.database) {
        return false
      }
      return this.modifyVisibility.is_public === this.database.is_public
    },
    canModifyVisibility () {
      if (!this.isOwner) {
        return false
      }
      return this.roles.includes('modify-database-visibility')
    },
    canModifyOwnership () {
      if (!this.isOwner) {
        return false
      }
      return this.roles.includes('modify-database-owner')
    },
    canUpdateScheme () {
      if (!this.isOwner) {
        return false
      }
      return this.roles.includes('find-database')
    },
    canModifyAccess () {
      if (!this.isOwner) {
        return false
      }
      return this.roles.includes('update-database-access')
    },
    canCreateAccess () {
      if (!this.isOwner) {
        return false
      }
      return this.roles.includes('create-database-access')
    },
    canModifyImage () {
      if (!this.isOwner) {
        return false
      }
      return this.roles.includes('modify-database-image')
    },
    databaseImage () {
      if (this.file) {
        return URL.createObjectURL(this.file)
      }
      if (!this.database || !this.database.image) {
        return null
      }
      return `data:image/webp;base64,${this.database.image}`
    },
    maxWidth () {
      return this.$config.public.database.image.width
    },
    maxHeight () {
      return this.$config.public.database.image.height
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
      this.modifyOwner.id = this.database.owner.id
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
    this.modifyOwner.id = this.database.owner.id
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
    closeDialog () {
      this.cacheStore.reloadDatabase()
      this.editAccessDialog = false
    },
    updateDatabaseVisibility () {
      this.loading = true
      const databaseService = useDatabaseService()
      databaseService.updateVisibility(this.$route.params.database_id, this.modifyVisibility)
        .then((database) => {
          const toast = useToastInstance()
          toast.success(this.$t('success.database.visibility'))
          this.cacheStore.setDatabase(database)
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    uploadFile () {
      this.loadingUpload = true
      console.debug('upload file', this.file)
      const uploadService = useUploadService()
      uploadService.create(this.file)
        .then((s3key) => {
          console.debug('uploaded image', s3key)
          const cacheStore = useCacheStore()
          cacheStore.setUploadProgress(null)
          const toast = useToastInstance()
          toast.success(this.$t('success.database.upload'))
          this.modifyImage.key = s3key
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
        })
        .catch(() => {
          const toast = useToastInstance()
          toast.error('Failed to modify image')
          this.loadingImage = false
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
        .catch(({code}) => {
          this.loadingDeleteImage = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
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
      databaseService.updateOwner(this.$route.params.database_id, { id: this.modifyOwner.id })
        .then(() => {
          const toast = useToastInstance()
          toast.success(this.$t('success.database.transfer'))
          this.$router.push(`/database/${this.$route.params.database_id}/info`)
        })
        .catch(() => {
          this.loading = false
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
            .catch(({code}) => {
              this.loadingSchema = false
              const toast = useToastInstance()
              if (typeof code !== 'string') {
                return
              }
              toast.error(this.$t(code))
            })
        })
        .catch(({code}) => {
          this.loadingSchema = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
    },
    giveAccess () {
      this.userId = null
      this.accessType = null
      this.editAccessDialog = true
    },
    modifyAccess (item) {
      this.userId = item.user.id
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
        .catch(() => {
          this.loadingUsers = false
        })
        .finally(() => {
          this.loadingUsers = false
        })
    }
  }
}
</script>
