<template>
  <div>
    <DBToolbar ref="toolbar" />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card v-if="isCreator" flat tile>
          <v-card-title>Modify database access</v-card-title>
          <v-card-subtitle>This is a dangerous operation</v-card-subtitle>
          <v-data-table
            :headers="headers"
            :items="database.accesses"
            :items-per-page="10">
            <template v-if="isCreator" v-slot:item.user="{ item }">
              {{ item.user.username }}
            </template>
            <template v-if="isCreator" v-slot:item.action="{ item }">
              <v-btn
                :disabled="isCreator && item.user.username === user.username"
                x-small
                @click="modifyAccess(item)">
                Modify
              </v-btn>
            </template>
          </v-data-table>
          <v-card-text>
            <v-btn
              small
              color="info"
              class="black--text"
              @click="giveAccess">
              Give Access
            </v-btn>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card v-if="isCreator" flat tile>
          <v-card-title>Modify database visibility</v-card-title>
          <v-card-subtitle>This is a dangerous operation</v-card-subtitle>
          <v-card-text>
            <v-alert
              v-if="database.is_public !== modifyVisibility.is_public"
              border="left"
              color="warning">
              <strong>Dangerous operation:</strong> you are about to change the visibility of the database. This affects all (sensitive) data held in the database.
            </v-alert>
            <v-row dense>
              <v-col>
                <v-switch
                  id="public"
                  v-model="modifyVisibility.is_public"
                  color="grey"
                  :label="publicLabel"
                  name="public" />
              </v-col>
            </v-row>
            <v-btn
              small
              :disabled="database.is_public === modifyVisibility.is_public"
              color="warning"
              @click="updateDatabaseVisibility">
              Modify Visibility
            </v-btn>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
    <v-dialog
      v-model="editAccessDialog"
      max-width="640">
      <EditAccess :database="database" :access="access" @close-dialog="closeDialog" />
    </v-dialog>
  </div>
</template>

<script>
import DBToolbar from '@/components/DBToolbar'
import EditAccess from '@/components/dialogs/EditAccess'
import { decodeJwt } from 'jose'

export default {
  components: {
    DBToolbar,
    EditAccess
  },
  data () {
    return {
      dialogDelete: false,
      confirm: null,
      loading: false,
      editAccessDialog: false,
      editVisibilityDialog: false,
      access: null,
      modifyVisibility: {
        is_public: null
      },
      headers: [
        { text: 'Username', value: 'user', sortable: false },
        { text: 'Access', value: 'type', sortable: false },
        { text: 'Action', value: 'action', sortable: false }
      ],
      accesses: [],
      user: {
        username: null
      },
      database: {
        id: null,
        name: null,
        description: null,
        is_public: null,
        created: null,
        contact: null,
        accesses: [],
        identifier: {
          id: null,
          license: {
            identifier: null,
            uri: null
          }
        },
        container: {
          id: null,
          name: null,
          internal_name: null
        },
        license: {
          uri: null,
          identifier: null
        },
        creator: {
          titles_before: null,
          firstname: null,
          lastname: null,
          username: null,
          titles_after: null
        }
      },
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tab () {
      return 0
    },
    db () {
      return this.$store.state.db
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
    },
    isCreator () {
      if (this.database.creator.username === null || this.user.username === null) {
        return false
      }
      return this.database.creator.username === this.user.username
    },
    publicLabel () {
      return this.modifyVisibility.is_public ? 'Public' : 'Private'
    }
  },
  mounted () {
    this.loadDatabase()
    this.loadUser()
  },
  methods: {
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        this.modifyVisibility.is_public = this.database.is_public
        console.debug('database', res.data)
      } catch (err) {
        this.$toast.error('Could not load database')
      }
      this.loading = false
    },
    loadUser () {
      if (!this.token) {
        return
      }
      this.user.username = decodeJwt(this.token).sub
    },
    closeDialog (event) {
      if (event.success) {
        this.loadDatabase()
      }
      this.loadDatabase()
      this.editAccessDialog = false
    },
    async updateDatabaseVisibility () {
      try {
        this.loading = true
        const res = await this.$axios.put(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/transfer`, this.modifyVisibility, this.config)
        this.database = res.data
        console.debug('database', this.database)
        this.$toast.success('Successfully updated the database')
        await this.$refs.toolbar.loadDatabase()
      } catch (err) {
        this.$toast.error('Failed to update database')
      }
      this.loading = false
    },
    giveAccess () {
      this.access = null
      this.editAccessDialog = true
    },
    modifyAccess (item) {
      this.access = item
      this.editAccessDialog = true
    }
  }
}
</script>
