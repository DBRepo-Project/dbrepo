<template>
  <div>
    <SubsetToolbar />
    <v-card flat tile>
      <Summary v-if="hasIdentifier" :identifier="identifier" />
      <v-card-text v-if="hasIdentifier">
        <Select :identifiers="identifiers" :identifier="identifier" />
      </v-card-text>
    </v-card>
    <v-divider v-if="subset && identifier" />
    <v-card flat tile>
      <v-card-title>Subset</v-card-title>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title>
                Subset Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-small" />
                <span v-if="database" v-text="database.is_public ? 'Public' : 'Private'" />
              </v-list-item-content>
              <v-list-item-title>
                Subset Creator
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!subset" type="text" class="skeleton-small" />
                <UserBadge v-if="subset" :user="subset.creator" :other-user="user" />
              </v-list-item-content>
              <v-list-item-title>
                Subset Query
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!subset" type="text" />
                <pre v-if="subset">{{ subset.query }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Query Hash
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!subset" type="text" />
                <pre v-if="subset">sha256:{{ subset.query_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Creation
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!executionUTC" type="text" class="skeleton-small" />
                <span v-if="executionUTC">{{ executionUTC }}</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Hash
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!subset" type="text" />
                <pre v-if="subset">{{ result_hash }}</pre>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Subset Count
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!subset" type="text" class="skeleton-xsmall" />
                <span v-if="subset">{{ subset.result_number }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
      <v-divider />
      <v-card-title>Database</v-card-title>
      <v-card-text>
        <v-list dense>
          <v-list-item>
            <v-list-item-content>
              <v-list-item-title>
                Database Visibility
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-small" />
                <span v-if="database">{{ database.is_public ? 'Public' : 'Private' }}</span>
              </v-list-item-content>
              <v-list-item-title class="mt-2">
                Database Name
              </v-list-item-title>
              <v-list-item-content>
                <v-skeleton-loader v-if="!database" type="text" class="skeleton-small" />
                <span v-if="database">{{ database.name }}</span>
              </v-list-item-content>
            </v-list-item-content>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>
<script>
import Summary from '@/components/identifier/Summary'
import SubsetToolbar from '@/components/query/SubsetToolbar.vue'
import QueryService from '@/api/query.service'
import Select from '@/components/identifier/Select'
import { formatTimestampUTCLabel } from '@/utils'
import UserMapper from '@/api/user.mapper'
import UserBadge from '@/components/UserBadge.vue'

export default {
  name: 'QueryShow',
  components: {
    Select,
    Summary,
    SubsetToolbar,
    UserBadge
  },
  data () {
    return {
      items: [
        { text: 'Databases', to: '/database', activeClass: '' },
        { text: `${this.$route.params.database_id}`, to: `/database/${this.$route.params.database_id}`, activeClass: '' },
        { text: 'Subsets', to: `/database/${this.$route.params.database_id}/query`, activeClass: '' },
        { text: `${this.$route.params.query_id}`, to: `/database/${this.$route.params.database_id}/query/${this.$route.params.query_id}`, activeClass: '' }
      ],
      persistQueryExists: false,
      persistQueryDialog: false,
      loadingDatabase: false,
      loadingIdentifier: false,
      loadingSubset: true,
      downloadLoading: false,
      error: false,
      promises: [],
      subset: null
    }
  },
  computed: {
    pid () {
      return this.$route.query.pid
    },
    database () {
      return this.$store.state.database
    },
    access () {
      return this.$store.state.access
    },
    user () {
      return this.$store.state.user
    },
    identifiers () {
      if (!this.database || !this.database.subsets || this.database.subsets.length === 0) {
        return []
      }
      return this.database.subsets.filter(s => s.query_id === Number(this.$route.params.query_id))
    },
    hasIdentifier () {
      return this.identifiers.length > 0
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
    title () {
      if (!this.hasIdentifier) {
        return null
      }
      const enTitle = this.identifier.titles.filter(t => t.language).filter(t => t.language === 'en')
      if (enTitle.length !== 1) {
        return this.identifier.titles[0].title
      }
      return enTitle[0].title
    },
    result_hash () {
      if (!this.subset.result_hash) {
        return '(none)'
      }
      return `sha256:${this.subset.result_hash}`
    },
    publisher () {
      if (this.database.publisher === null) {
        return 'NA'
      }
      return this.database.publisher
    },
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
    }
  },
  mounted () {
    this.loadSubset()
  },
  methods: {
    loadSubset () {
      this.loadingSubset = true
      QueryService.findOne(this.$route.params.database_id, this.$route.params.query_id)
        .then((subset) => {
          this.subset = subset
        })
        .catch(() => {
          this.loadingSubset = false
        })
        .finally(() => {
          this.loadingSubset = false
        })
    },
    isCreator (subset) {
      if (!this.user) {
        return false
      }
      return subset.creator.id === this.user.id
    },
    formatCreator (creator) {
      return UserMapper.userToFullName(creator)
    }
  }
}
</script>

<style>
pre {
  white-space: break-spaces;
}
.v-card__text {
  font-size: initial;
}
.skeleton-large > div {
  width: 400px !important;
}
.skeleton-medium > div {
  width: 200px !important;
}
.skeleton-small > div {
  width: 100px !important;
}
.skeleton-xsmall > div {
  width: 50px !important;
}
.v-data-table {
  border-radius: 0;
}
#back-btn {
  min-width: auto;
  padding: 0 0 0 12px;
  background: none !important;
  box-shadow: none;
}
#back-btn::before {
  opacity: 0;
}
</style>
