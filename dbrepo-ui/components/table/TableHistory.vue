<template>
  <div>
    <v-card
      :title="$t('pages.table.subpages.versioning.title')"
      :subtitle="$t('pages.table.subpages.versioning.subtitle')"
      variant="elevated">
      <v-card-text>
        <v-text-field
          v-model="datetime"
          required
          :rules="[
            v => !!v || $t('validation.required'),
            v => v && /^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$/.test(v) || $t('validation.pattern.timestamp')]"
          persistent-hint
          :variant="inputVariant"
          :label="$t('pages.table.subpages.versioning.timestamp.label')"
          :hint="$t('pages.table.subpages.versioning.timestamp.hint')"
          :placeholder="$t('pages.table.subpages.versioning.timestamp.placeholder')"
          :suffix="$t('pages.table.subpages.versioning.timestamp.suffix')"
          class="mb-4"
          type="text" />
        <Loading
          v-if="loading" />
        <Bar
          v-if="!loading"
          id="time-travel"
          :data="chartData"
          :options="chartOptions"
          :height="200"
          :width="400" />
        <pre>{{ history }}</pre>
        <p>
          {{ $t('pages.table.subpages.versioning.chart.legend') }}
        </p>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          :variant="buttonVariant"
          :text="$t('navigation.cancel')"
          @click="cancel" />
        <v-btn
          color="tertiary"
          :variant="buttonVariant"
          :text="$t('navigation.now')"
          @click="reset" />
        <v-btn
          color="primary"
          variant="flat"
          :disabled="datetime === null || datetime === undefined || datetime === ''"
          :text="$t('navigation.continue')"
          @click="pick" />
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
import { UTCDate } from '@date-fns/utc'
import { Bar } from 'vue-chartjs'
import { format } from 'date-fns'
import { Chart as ChartJS, Title, Tooltip, BarElement, CategoryScale, LinearScale, LogarithmicScale } from 'chart.js'

ChartJS.register(Title, Tooltip, BarElement, CategoryScale, LinearScale, LogarithmicScale)

export default {
  components: {
    Bar
  },
  data () {
    return {
      formValid: false,
      loading: true,
      datetime: null,
      history: null,
      chartData: null,
      chartOptions: {
        responsive: true,
        onClick: this.handle,
        animations: null,
        plugins: {
          title: {
            display: true,
            text: this.$t('pages.table.subpages.versioning.chart.title')
          }
        },
        scales: {
          y: {
            display: true,
            ticks: {
              min: 0,
              stepSize: 1
            },
            title: {
              display: true,
              text: this.$t('pages.table.subpages.versioning.chart.ylabel')
            },
          },
          x: {
            display: false,
            ticks: {
              min: 0,
              stepSize: 1
            },
            title: {
              display: true,
              text: this.$t('pages.table.subpages.versioning.chart.xlabel')
            }
          }
        }
      },
      cacheStore: useCacheStore()
    }
  },
  computed: {
    table () {
      return this.cacheStore.getTable
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    }
  },
  mounted() {
    this.loadHistory()
  },
  methods: {
    cancel () {
      this.$emit('close', { success: false })
    },
    reset () {
      this.$emit('close', { success: true, timestamp: null })
    },
    pick () {
      this.$emit('close', {
        success: true,
        timestamp: this.datetime
      })
    },
    handle (point, event) {
      if (event.length !== 1 || event[0].index === undefined) {
        return
      }
      const idx = event[0].index
      this.datetime = this.chartData.labels[idx]
      console.debug('date time', this.datetime, 'idx', idx)
    },
    filterHistoryEventType (history, type) {
      return history.map(d => {
        if (d.event === type) {
          return d.total
        }
        return null
      })
    },
    loadHistory () {
      this.loading = true
      const tableService = useTableService()
      tableService.history(this.table.database_id, this.table.id)
        .then((history) => {
          this.loading = false
          this.chartData = {
            // labels: history ? history.map(d => format(new UTCDate(d.timestamp), 'yyyy-MM-dd HH:mm:ss.SSS')) : [],
            labels: history ? history.map(d => format(new UTCDate(d.timestamp), 'yyyy-MM-dd HH:mm:ss')) : [],
            datasets: [
              { backgroundColor: this.$vuetify.theme.current.colors.success, data: this.filterHistoryEventType(history, 'insert') },
              { backgroundColor: this.$vuetify.theme.current.colors.error, data: this.filterHistoryEventType(history, 'delete') }
            ]
          }
        })
        .catch(({code, message}) => {
          this.loading = false
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(message)
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
