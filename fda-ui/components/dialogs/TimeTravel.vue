<template>
  <div>
    <v-card>
      <v-progress-linear v-if="loading" :color="loadingColor" :indeterminate="!error" />
      <v-card-title>
        Versioning
      </v-card-title>
      <v-card-subtitle>
        Choose a timestamp, the chart shows when changes occurred.
      </v-card-subtitle>
      <v-card-text>
        <v-text-field
          v-model="datetime"
          label="Timestamp"
          type="datetime-local" />
        <p v-if="totalChanges > 0">
          The are {{ totalChanges }} total changes in the dataset:
        </p>
        <v-sparkline
          v-if="!loading && totalChanges > 0"
          :labels="labels"
          :value="values"
          stroke-linecap="round"
          type="trend"
          color="primary"
          smooth="15"
          line-width="1"
          padding="10" />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          class="mb-2"
          @click="cancel">
          Cancel
        </v-btn>
        <v-btn
          class="mb-2"
          color="blue-grey white--text"
          @click="reset">
          Now
        </v-btn>
        <v-btn
          id="version"
          class="mb-2"
          :disabled="datetime === null || datetime === undefined || datetime === ''"
          color="primary"
          @click="pick">
          Pick
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import _ from 'lodash'
import { format } from 'date-fns'
export default {
  data () {
    return {
      formValid: false,
      loading: false,
      error: false,
      datetime: null,
      labels: [],
      values: [],
      totalChanges: 0
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    }
  },
  mounted () {
    this.loadHistory()
  },
  methods: {
    cancel () {
      this.$parent.$parent.$parent.$parent.pickVersionDialog = false
    },
    sleep (ms) {
      return new Promise((resolve) => {
        setTimeout(resolve, ms)
      })
    },
    reset () {
      this.$parent.$parent.$parent.$parent.version = null
      this.cancel()
    },
    pick () {
      this.$parent.$parent.$parent.$parent.version = this.formatDate()
      this.cancel()
    },
    formatDate () {
      if (this.datetime === null || this.datetime === undefined || this.datetime === '') {
        return null
      }
      console.debug('selected date', this.datetime)
      return Date.parse(this.datetime)
    },
    aggregateChanges (data) {
      const changes = _.map(data, o => o.length)
      changes.unshift(0)
      console.debug('mapped changes', changes)
      return changes
    },
    aggregateLabels (data) {
      const labels = _.map(data, (o) => {
        const first = _.head(o)
        if (first.deleted_at === null) {
          return format(new Date(first.inserted_at), 'dd.MM.')
        }
        return format(new Date(first.deleted_at), 'dd.MM.')
      })
      labels.unshift('*')
      console.debug('mapped labels', labels)
      return labels
    },
    async loadHistory () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/history`, {
          headers: this.requestHeaders
        })
        this.error = false
        let data = res.data
        this.totalChanges = data.length
        data = _.partition(data, o => o.inserted_at)
        data = _.reject(data, o => o.length === 0)
        console.debug('table history', data)
        this.values = this.aggregateChanges(data)
        this.labels = this.aggregateLabels(data)
      } catch (err) {
        this.error = true
        console.error('failed to load table history', err)
      }
      this.loading = false
    }
  }
}
</script>
