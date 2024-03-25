<template>
  <div>
    <v-card
      :title="$t('pages.table.subpages.versioning.title')"
      :subtitle="$t('pages.table.subpages.versioning.subtitle')"
      variant="elevated">
      <v-progress-linear v-if="loading" color="primary" />
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
        <Bar
          id="time-travel"
          :data="chartData"
          :options="chartOptions"
          :height="200"
          :width="400" />
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          :variant="buttonVariant"
          :text="$t('navigation.cancel')"
          @click="cancel" />
        <v-btn
          color="tertiary"
          variant="flat"
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
import { Bar } from 'vue-chartjs'
import { format } from 'date-fns'
import { useCacheStore } from '@/stores/cache'
import { Chart as ChartJS, Title, Tooltip, BarElement, CategoryScale, LinearScale, LogarithmicScale } from 'chart.js'

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
      history: null,
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
            type: 'logarithmic'
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
    },
    chartData () {
      return {
        labels: this.history ? this.history.map(d => format(new Date(d.timestamp), 'yyyy-MM-dd HH:mm:ss')) : [],
        datasets: [
          this.history ? { backgroundColor: this.$vuetify.theme.current.colors.primary, data: this.history.map(d => d.total) } : { data: [] }
        ]
      }
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
    async loadHistory () {
      try {
        this.loading = true
        const tableService = useTableService()
        this.history = await tableService.history(this.table.database_id, this.table.id)
        // this.chartData.labels = history.map(d => format(new Date(d.timestamp), 'dd.MM.yyyy HH:mm:ss'))
        // this.chartData.datasets = [{
        //   // backgroundColor: 'red',
        //   data: history.map(d => d.total)
        // }]
        this.totalChanges = history.length
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
