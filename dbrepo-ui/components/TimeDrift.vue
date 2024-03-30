<template>
  <v-alert
    v-cloak
    v-if="timestamp && offSeconds > 3"
    class="banner"
    border="start"
    tile
    type="warning">
    {{ $t('error.data.drift') + ' ' + offSeconds + 's' }}
  </v-alert>
</template>

<script>
import { formatTimestamp, timestampsToHumanDifference } from '@/utils'

export default {
  data () {
    return {
      timestamp: null
    }
  },
  computed: {
    drift () {
      return this.timestampsToHumanDifference(Date.now(), this.timestamp)
    },
    offSeconds () {
      if (!this.timestamp) {
        return null
      }
      return (Date.now().valueOf() - Date.parse(this.timestamp)) / 1000
    }
  },
  mounted() {
    const databaseService = useDatabaseService()
    databaseService.getServerTime()
      .then((timestamp) => {
        this.timestamp = timestamp
      })
  },
  methods: {
    formatTimestamp,
    timestampsToHumanDifference
  }
}
</script>

<script setup lang="ts">
</script>
