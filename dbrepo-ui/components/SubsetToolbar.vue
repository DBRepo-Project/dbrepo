<template>
  <div>
    <v-toolbar flat>
      <v-toolbar-title>
        <v-btn id="back-btn" class="mr-2" :to="`/database/${$route.params.database_id}/query`">
          <v-icon left>mdi-arrow-left</v-icon>
        </v-btn>
      </v-toolbar-title>
      <v-toolbar-title v-if="identifier" v-text="title" />
      <v-spacer />
      <v-toolbar-title>
        <v-btn v-if="canPersistQuery" :loading="loadingSave" class="mb-1" @click.stop="save">
          <v-icon left>mdi-content-save-outline</v-icon> Save
        </v-btn>
        <v-btn v-if="result_visibility && subset && subset.result_number" class="mb-1" :loading="downloadLoading" @click.stop="downloadSubset">
          <v-icon left>mdi-download</v-icon> Data .csv
        </v-btn>
        <DownloadButton v-if="identifier" :pid="identifier.id" class="mb-1">
          <v-icon left>mdi-code-tags</v-icon> PID .xml
        </DownloadButton>
        <v-btn v-if="canGetPid" class="mb-1" color="primary" :disabled="!executionUTC" :to="`/database/${$route.params.database_id}/query/${$route.params.query_id}/persist`">
          <v-icon left>mdi-content-save-outline</v-icon> Get PID
        </v-btn>
      </v-toolbar-title>
    </v-toolbar>
    <v-tabs v-model="tab" color="primary">
      <v-tab :to="`/database/${$route.params.database_id}/query/${$route.params.query_id}/info`">
        Info
      </v-tab>
      <v-tab :to="`/database/${$route.params.database_id}/query/${$route.params.query_id}/data`">
        Data
      </v-tab>
    </v-tabs>
  </div>
</template>

<script>
import UserUtils from '@/api/user.utils'
import QueryService from '@/api/query.service'
import DownloadButton from '@/components/identifier/DownloadButton.vue'
import IdentifierMapper from '@/api/identifier.mapper'
import { formatTimestampUTCLabel } from '@/utils'

export default {
  components: {
    DownloadButton
  },
  data () {
    return {
      tab: null,
      loading: false,
      loadingSave: false,
      downloadLoading: false,
      identifier: null,
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
    roles () {
      return this.$store.state.roles
    },
    identifiers () {
      if (!this.database || !this.database.subsets || this.database.subsets.length === 0) {
        return []
      }
      return this.database.subsets
    },
    canPersistQuery () {
      if (this.loading || !this.subset || this.subset.is_persisted) {
        return false
      }
      return UserUtils.hasReadAccess(this.access)
    },
    executionUTC () {
      if (!this.subset) {
        return null
      }
      return formatTimestampUTCLabel(this.subset.created)
    },
    result_visibility () {
      if (!this.database || this.database.is_public === null) {
        return false
      }
      if (this.database.is_public) {
        return true
      }
      return this.subset.creator.username === this.username
    },
    canGetPid () {
      if (!this.user || !this.subset || !this.database) {
        return false
      }
      return this.database.owner.id === this.user.id || this.subset.creator.id === this.user.id
    },
    title () {
      if (!this.identifier) {
        return null
      }
      return IdentifierMapper.identifierPreferEnglishTitle(this.identifier)
    }
  },
  mounted () {
    /* mount pid */
    if (this.pid) {
      const filter = this.identifiers.filter(i => i.id === Number(this.pid))
      if (filter.length > 0) {
        this.identifier = filter[0]
        console.debug('identifier set according to route pid', this.identifier)
        return
      }
    }
    this.identifier = this.identifiers[0]
    console.debug('defaulted to latest identifier', this.identifier)
    /* load subset metadata */
    if (!this.subset) {
      this.loadSubset()
    }
  },
  methods: {
    save () {
      this.loadingSave = true
      QueryService.persist(this.$route.params.database_id, this.$route.params.query_id)
        .then((subset) => {
          this.subset = subset
        })
        .finally(() => {
          this.loadingSave = false
        })
    },
    loadSubset () {
      this.loading = true
      QueryService.findOne(this.$route.params.database_id, this.$route.params.query_id)
        .then((subset) => {
          this.subset = subset
        })
        .finally(() => {
          this.loading = false
        })
    },
    downloadSubset () {
      this.downloadLoading = true
      QueryService.exportSubset(this.$route.params.database_id, this.$route.params.query_id)
        .then((data) => {
          const url = window.URL.createObjectURL(new Blob([data]))
          const link = document.createElement('a')
          link.href = url
          link.setAttribute('download', 'subset.csv')
          document.body.appendChild(link)
          link.click()
        })
        .finally(() => {
          this.downloadLoading = false
        })
    }
  }
}
</script>
