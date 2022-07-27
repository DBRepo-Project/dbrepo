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
          required
          :rules="[v => !!v || $t('Required'), v => v && /^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$/.test(v) || $t('Please us the pattern yyyy-MM-dd HH:mm:ss')]"
          hint="e.g. 2022-07-04 12:53:00"
          suffix="UTC"
          class="mb-4"
          type="text" />
        The following chart summarizes changes (insert/update/delete) in the dataset and give an indication where
        versions of interest may be.
        <Bar
          chart-id="time-travel"
          :chart-data="chartData"
          :chart-options="chartOptions"
          dataset-id-key="label"
          :height="80"
          :width="400" />
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
import { Bar } from 'vue-chartjs/legacy'
import { Chart as ChartJS, Title, Tooltip, BarElement, CategoryScale, LinearScale, LogarithmicScale } from 'chart.js'
import { formatTimestampUTC, formatTimestampUTCLabel } from '@/utils'

ChartJS.register(Title, Tooltip, BarElement, CategoryScale, LinearScale, LogarithmicScale)

export default {
  components: {
    Bar
  },
  data () {
    return {
      formValid: false,
      loading: false,
      error: false,
      datetime: null,
      chartData: {
        labels: [],
        datasets: [],
        dates: []
      },
      chartOptions: {
        responsive: true,
        onClick: this.handle,
        scales: {
          y: {
            display: true,
            type: 'logarithmic'
          }
        }
      },
      totalChanges: 0
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
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
    handle (point, event) {
      if (event.length !== 1 || event[0].index === undefined) {
        return
      }
      const idx = event[0].index
      this.datetime = this.chartData.dates[idx]
      console.debug('date time', this.datetime, 'idx', idx)
    },
    reset () {
      this.$parent.$parent.$parent.$parent.version = null
      this.cancel()
    },
    pick () {
      this.$emit('close', {
        time: this.datetime
      })
    },
    async loadHistory () {
      try {
        this.loading = true
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/table/${this.$route.params.table_id}/history`, this.config)
        this.error = false
        this.chartData.labels = res.data.map(function (d, idx) {
          if (idx === 0) {
            return 'Origin'
          }
          return formatTimestampUTCLabel(d.timestamp)
        })
        this.chartData.dates = res.data.map(d => formatTimestampUTC(d.timestamp))
        this.chartData.datasets = [{
          backgroundColor: this.$vuetify.theme.themes.light.primary,
          data: res.data.map(d => d.total)
        }]
        // this.totalChanges = this.res.data.length
        console.debug('history', this.chartData)
      } catch (err) {
        this.error = true
        console.error('failed to load table history', err)
      }
      this.loading = false
    }
  }
}
</script>
