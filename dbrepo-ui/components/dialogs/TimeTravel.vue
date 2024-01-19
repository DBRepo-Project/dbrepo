<template>
  <div>
    <v-card>
      <v-progress-linear v-if="loading" color="primary" />
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
import TableService from '@/api/table.service'
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
  mounted () {
    this.loadHistory()
  },
  methods: {
    cancel () {
      this.$emit('close', { success: false })
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
    loadHistory () {
      this.loading = true
      TableService.findHistory(this.$route.params.database_id, this.$route.params.table_id)
        .then((history) => {
          this.chartData.labels = history.map(function (d, idx) {
            if (idx === 0) {
              return 'Origin'
            }
            return formatTimestampUTCLabel(d.timestamp)
          })
          this.chartData.dates = history.map(d => formatTimestampUTC(d.timestamp))
          this.chartData.datasets = [{
            backgroundColor: this.$vuetify.theme.themes.light.primary,
            data: history.map(d => d.total)
          }]
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
