<template>
  <div v-if="user">
    <DatabaseToolbar ref="toolbar" />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="canModifyImage" flat tile>
          <v-card-title>Image</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col>
                The image will be displayed in a box with maximum dimensions 200x200 pixels.
              </v-col>
            </v-row>
            <v-row dense>
              <v-col sm="6">
                <v-file-input
                  v-model="fileModel"
                  accept="image/*"
                  hint="max. 1MB file size"
                  persistent-hint
                  clearable
                  :loading="loadingUpload"
                  :show-size="1000"
                  counter
                  label="Teaser Image"
                  @change="uploadFile" />
              </v-col>
            </v-row>
            <v-btn
              small
              class="black--text mt-4"
              :loading="loadingImage"
              @click="updateDatabaseImage">
              Modify Image
            </v-btn>
            <v-btn
              v-if="database.image"
              small
              color="warning"
              class="black--text mt-4"
              :loading="loadingDeleteImage"
              @click="removeDatabaseImage">
              Remove Image
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="isOwner" flat tile>
          <v-card-title>Access</v-card-title>
          <v-data-table
            :headers="headers"
            :items="database.accesses"
            :items-per-page="10">
            <template v-slot:item.qualified_name="{ item }">
              <span v-if="item && item.user" v-text="item.user.qualified_name" />
            </template>
            <template v-slot:item.action="{ item }">
              <v-btn
                v-if="item && item.user && item.user.username !== user.username"
                x-small
                :disabled="!canModifyAccess"
                @click="modifyAccess(item)">
                Modify
              </v-btn>
            </template>
          </v-data-table>
          <v-card-text>
            <v-btn
              small
              :disabled="!canCreateAccess"
              color="warning"
              class="black--text"
              @click="giveAccess">
              Give Access
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="canModifyVisibility" flat tile>
          <v-card-title>Visibility</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col sm="6">
                <v-select
                  id="visibility"
                  v-model="modifyVisibility.is_public"
                  :items="visibility"
                  label="Visibility"
                  name="visibility" />
              </v-col>
            </v-row>
            <v-btn
              small
              color="warning"
              class="black--text"
              :disabled="isSameVisibility"
              @click="updateDatabaseVisibility">
              Modify Visibility
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="canModifyOwnership" flat tile>
          <v-card-title>Ownership</v-card-title>
          <v-card-text>
            <v-row dense>
              <v-col sm="6">
                <v-select
                  id="owner"
                  v-model="modifyOwner.id"
                  :items="users"
                  item-text="username"
                  item-value="id"
                  label="Owner"
                  name="owner" />
              </v-col>
            </v-row>
            <v-btn
              small
              color="warning"
              class="black--text"
              :disabled="isSameOwner"
              @click="updateDatabaseOwner">
              Modify Ownership
            </v-btn>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="editAccessDialog"
      max-width="640">
      <EditAccess :user-id="userId" :access-type="accessType" @close-dialog="closeDialog" />
    </v-dialog>
  </div>
</template>

<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import EditAccess from '@/components/dialogs/EditAccess.vue'
import DatabaseService from '@/api/database.service'
import UserService from '@/api/user.service'
import UploadService from '@/api/upload.service'

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
      loadingDeleteImage: false,
      fileModel: null,
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
        { text: 'Public', value: true },
        { text: 'Private', value: false }
      ],
      headers: [
        { text: 'Name', value: 'qualified_name', sortable: false },
        { text: 'Access', value: 'type', sortable: false },
        { text: 'Action', value: 'action', sortable: false }
      ],
      accesses: [],
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tab () {
      return 0
    },
    database () {
      return this.$store.state.database
    },
    access () {
      return this.$store.state.access
    },
    token () {
      return this.$store.state.token
    },
    roles () {
      return this.$store.state.roles
    },
    config () {
      if (this.token === null) {
        return {}
      }
      return {
        headers: { Authorization: `Bearer ${this.token}` }
      }
    },
    user () {
      return this.$store.state.user
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
    closeDialog (event) {
      this.reloadDatabase()
      this.editAccessDialog = false
    },
    updateDatabaseVisibility () {
      this.loading = true
      DatabaseService.modifyVisibility(this.$route.params.database_id, this.modifyVisibility.is_public)
        .then(() => {
          this.$toast.success('Successfully updated the database visibility')
          location.reload()
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
      UploadService.upload(this.$config.uploadEndpointUrl, this.fileModel)
        .then((metadata) => {
          console.debug('uploaded image', metadata)
          this.modifyImage.key = metadata.s3key
          this.loadingUpload = false
        })
        .finally(() => {
          this.loadingUpload = false
        })
    },
    updateDatabaseImage () {
      this.loadingImage = true
      DatabaseService.modifyImage(this.$route.params.database_id, this.modifyImage)
        .then(() => {
          this.$toast.success('Updated image successfully')
          this.$store.dispatch('reloadDatabase')
          this.loadingImage = false
        })
        .catch(() => {
          this.$toast.error('Failed to modify image')
          this.loadingImage = false
        })
        .finally(() => {
          this.loadingImage = false
        })
    },
    removeDatabaseImage () {
      this.loadingDeleteImage = true
      DatabaseService.modifyImage(this.$route.params.database_id, { key: null })
        .then(() => {
          this.$toast.success('Removed image successfully')
          this.$store.dispatch('reloadDatabase')
          this.loadingDeleteImage = false
        })
        .catch(() => {
          this.$toast.error('Failed to delete image')
          this.loadingDeleteImage = false
        })
        .finally(() => {
          this.loadingDeleteImage = false
        })
    },
    updateDatabaseOwner () {
      this.loading = true
      DatabaseService.modifyOwner(this.$route.params.database_id, this.modifyOwner.username)
        .then(() => {
          this.$toast.success('Successfully updated the database owner')
          location.reload()
        })
        .catch(() => {
          this.loading = false
        })
        .finally(() => {
          this.loading = false
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
      UserService.findAll()
        .then((users) => {
          this.users = users
        })
        .catch(() => {
          this.loadingUsers = false
        })
        .finally(() => {
          this.loadingUsers = false
        })
    },
    reloadDatabase () {
      this.$store.dispatch('reloadDatabase')
    }
  }
}
</script>
