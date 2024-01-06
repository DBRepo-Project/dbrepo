<template>
  <div>
    <v-list-item-group v-model="idx" color="primary">
      <v-list-item v-for="(id,i) in identifiers" :key="i" :href="href(id)" two-line>
        <v-list-item-content>
          <v-list-item-title>{{ formatTimestampUTCLabel(id.created) }}</v-list-item-title>
          <v-list-item-subtitle>
            <Banner :identifier="id" />
          </v-list-item-subtitle>
        </v-list-item-content>
        <v-list-item-action>
          <v-btn v-if="canDeleteIdentifier" color="error" x-small @click="deleteDialog = true">Delete PID</v-btn>
          <v-tooltip v-else left>
            <template v-slot:activator="{ on, attrs }">
              <v-icon color="primary" v-bind="attrs" v-on="on">mdi-identifier</v-icon>
            </template>
            Persistent identifier
          </v-tooltip>
        </v-list-item-action>
      </v-list-item>
    </v-list-item-group>
    <v-dialog
      v-model="deleteDialog"
      persistent
      max-width="480">
      <DeleteIdentifier :identifier="localIdentifier" @close="closeDeleteDialog" />
    </v-dialog>
  </div>
</template>
<script>
import Banner from '@/components/identifier/Banner'
import { formatTimestampUTCLabel } from '@/utils'
import DeleteIdentifier from '@/components/dialogs/DeleteIdentifier'

export default {
  components: {
    DeleteIdentifier,
    Banner
  },
  props: {
    identifiers: {
      type: Array,
      default () {
        return []
      }
    },
    identifier: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
      idx: null,
      deleteDialog: false,
      localIdentifier: null
    }
  },
  computed: {
    user () {
      return this.$store.state.user
    },
    roles () {
      return this.$store.state.roles
    },
    canDeleteIdentifier () {
      if (!this.user) {
        return false
      }
      return this.roles.includes('delete-identifier')
    }
  },
  watch: {
    identifier: {
      handler () {
        this.init()
      },
      deep: true
    },
    idx: {
      handler () {
        this.localIdentifier = this.identifiers[this.idx]
      }
    }
  },
  mounted () {
    this.init()
  },
  methods: {
    formatTimestampUTCLabel,
    href (identifier) {
      if (this.canDeleteIdentifier) {
        return null
      }
      return `/pid/${identifier.id}`
    },
    async closeDeleteDialog (event) {
      if (event.action === 'deleted') {
        await this.$store.dispatch('reloadDatabase')
      }
      this.deleteDialog = false
    },
    init () {
      if (!this.identifiers || this.identifiers.length === 0 || !this.identifier) {
        return null
      }
      this.idx = this.identifiers.map(i => i.id).indexOf(this.identifier.id)
      this.localIdentifier = this.identifier
    }
  }
}
</script>
