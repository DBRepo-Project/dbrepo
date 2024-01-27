<template>
  <div v-if="database">
    <DatabaseToolbar />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <Summary v-if="hasIdentifier" :identifier="identifier" />
        <v-card v-if="hasIdentifier" flat tile>
          <v-card-text>
            <Select :identifiers="identifiers" :identifier="identifier" />
          </v-card-text>
        </v-card>
        <v-divider v-if="hasIdentifier" />
        <v-card flat tile>
          <v-card-title>Database</v-card-title>
          <v-card-text>
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Name
                  </v-list-item-title>
                  <v-list-item-content v-if="database" v-text="database.name" />
                  <v-list-item-title class="mt-2">
                    Database Internal Name
                  </v-list-item-title>
                  <v-list-item-content v-if="database" v-text="database.internal_name" />
                  <v-list-item-title>
                    Database Visibility
                  </v-list-item-title>
                  <v-list-item-content v-if="database" v-text="`${database.is_public ? 'Public' : 'Private'}`" />
                  <v-list-item-title>
                    Database Size
                  </v-list-item-title>
                  <v-list-item-content v-if="databaseSize" v-text="databaseSize" />
                  <v-list-item-title class="mt-2">
                    Database Owner
                  </v-list-item-title>
                  <v-list-item-content>
                    <UserBadge v-if="database" :user="database.owner" :other-user="user" />
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Database Creation
                  </v-list-item-title>
                  <v-list-item-content v-if="!loading" v-text="createdUTC" />
                  <v-list-item-title v-if="access && access.type" class="mt-2">
                    Database Access
                  </v-list-item-title>
                  <v-list-item-content v-if="access && access.type">
                    <span>
                      <v-badge v-if="databaseExtraInfo" inline :content="databaseExtraInfo" color="secondary">
                        <span v-text="accessDescription.text" />
                      </v-badge>
                      <span v-else v-text="accessDescription.text" />
                    </span>
                  </v-list-item-content>
                  <v-list-item-title v-if="access" class="mt-2">
                    Database Connection
                  </v-list-item-title>
                  <v-list-item-content v-if="access">
                    <pre class="pb-1" v-text="jdbcString" />
                  </v-list-item-content>
                  <v-list-item-title v-if="contact" class="mt-2">
                    Database Contact
                  </v-list-item-title>
                  <v-list-item-content>
                    <UserBadge v-if="database.contact" :user="database.contact" :other-user="user" />
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
        <v-divider />
        <v-card flat tile>
          <v-card-title>Container</v-card-title>
          <v-card-text>
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Container Name
                  </v-list-item-title>
                  <v-list-item-content v-if="!loading" v-text="container_name" />
                  <v-list-item-title class="mt-2">
                    Container Internal Name
                  </v-list-item-title>
                  <v-list-item-content v-if="!loading" v-text="container_internal_name" />
                  <v-list-item-title class="mt-2">
                    Image Name
                  </v-list-item-title>
                  <v-list-item-content v-if="!loading" v-text="image_name" />
                  <v-list-item-title class="mt-2">
                    Image Version
                  </v-list-item-title>
                  <v-list-item-content v-if="!loading" v-text="image_version" />
                </v-list-item-content>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DatabaseToolbar from '@/components/database/DatabaseToolbar.vue'
import { formatTimestampUTCLabel, sizeToHumanLabel } from '@/utils'
import DatabaseMapper from '@/api/database.mapper'
import Summary from '@/components/identifier/Summary'
import Select from '@/components/identifier/Select'
import UserBadge from '@/components/UserBadge.vue'

export default {
  components: {
    DatabaseToolbar,
    Summary,
    Select,
    UserBadge
  },
  data () {
    return {
      loading: false,
      loadingStart: false,
      loadingStop: false,
      editDialog: false,
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
    description () {
      if (!this.hasIdentifier) {
        return ''
      }
      return this.database.identifier.description
    },
    publisher () {
      if (!this.hasIdentifier) {
        return ''
      }
      return this.database.identifier.publisher
    },
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    identifiers () {
      if (!this.database) {
        return []
      }
      return this.database.identifiers
    },
    identifier () {
      if (this.pid) {
        const filter = this.identifiers.filter(i => i.id === Number(this.pid))
        if (filter.length > 0) {
          return filter[0]
        }
      }
      return this.identifiers[0]
    },
    access () {
      return this.$store.state.access
    },
    database () {
      return this.$store.state.database
    },
    pid () {
      return this.$route.query.pid
    },
    createdUTC () {
      return formatTimestampUTCLabel(this.database.created)
    },
    internal_name () {
      return this.database.internal_name
    },
    container_name () {
      return this.database.container.name
    },
    container_internal_name () {
      return this.database.container.internal_name
    },
    image_name () {
      return this.database.container.image.name
    },
    image_version () {
      return this.database.container.image.version
    },
    contact () {
      return DatabaseMapper.databaseToContact(this.database)
    },
    owner () {
      return DatabaseMapper.databaseToOwner(this.database)
    },
    hasIdentifier () {
      return this.identifiers.length > 0
    },
    accessDescription () {
      if (!this.access) {
        return
      }
      switch (this.access.type) {
        case 'read':
          return { text: 'You can read all contents' }
        case 'write_own':
          return { text: 'You can write own tables and read all contents' }
        case 'write_all':
          return { text: 'You have full access' }
        default:
          return { text: null, class: null }
      }
    },
    jdbcString () {
      const flags = this.database.container.ui_additional_flags ? this.database.container.ui_additional_flags : ''
      return `jdbc:${this.database.container.image.jdbc_method}://${this.database.container.ui_host}:${this.database.container.ui_port}/${this.database.internal_name}${flags} (username=${this.user.username}, password=yourpassword)`
    },
    databaseExtraInfo () {
      return this.$config.databaseExtraInfo
    },
    databaseSize () {
      if (!this.database) {
        return null
      }
      let sum = 0
      this.database.tables.forEach((t) => { sum += t.data_length })
      return sizeToHumanLabel(sum)
    }
  },
  methods: {
    sizeToHumanLabel
  }
}
</script>
<style>
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
.current-identifier {
  background: #1976d2;
}
</style>
