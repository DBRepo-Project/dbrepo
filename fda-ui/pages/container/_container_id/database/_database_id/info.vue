<template>
  <div v-if="db">
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-if="!loading" v-model="tab">
      <v-tab-item>
        <v-card flat>
          <v-card-text>
            <p>
              <strong>DataCite Metadata</strong>
            </p>
            <p v-if="db.publisher">
              Publisher: {{ db.publisher }}
            </p>
            <p v-if="db.publication_year">
              Year: <code>{{ db.publication_year }}</code>
            </p>
            <p v-if="db.subjects">
              Subjects: <code class="mr-1" v-for="(subject,idx) in db.subjects" :key="idx">{{ subject }}</code>
            </p>
            <p v-if="db.language">
              Language: <code>{{ db.language }}</code>
            </p>
            <p v-if="db.license">
              License: <a :href="db.license.uri">
                <code>{{ db.license.identifier }}</code>
              </a>
            </p>
            <p v-if="db.description">
              Description: {{ db.description }}
            </p>
            <p>
            </p>
            <p class="mt-2">
              <strong>Technical Information</strong>
            </p>
            <p>
              Database: <code>{{ db.internal_name }}</code>
            </p>
            <p>
              Image: <code>{{ db.image.repository }}:{{ db.image.tag }}</code>
            </p>
            <p>
              Created: <code>{{ formatDate(db.created) }}</code>
            </p>
          </v-card-text>
        </v-card>
      </v-tab-item>
    </v-tabs-items>
    <v-breadcrumbs :items="items" class="pa-0 mt-2" />
  </div>
</template>

<script>
import DBToolbar from '@/components/DBToolbar'
import { format } from 'date-fns'

export default {
  components: {
    DBToolbar
  },
  data () {
    return {
      loading: false,
      items: [
        { text: 'Databases', to: '/container', activeClass: '' },
        {
          text: `${this.$route.params.database_id}`,
          to: `/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}/info`,
          activeClass: ''
        }
      ]
    }
  },
  computed: {
    tab () {
      return 0
    },
    db () {
      return this.$store.state.db
    }
  },
  mounted () {
    this.init()
  },
  methods: {
    async init () {
      this.loading = true
      if (this.db != null && this.db.id === this.$route.params.database_id) {
        this.loading = false
        return
      }
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`)
        console.debug('database', res.data)
        this.$store.commit('SET_DATABASE', res.data)
        this.loading = false
      } catch (err) {
        this.$toast.error('Could not load database.')
        this.loading = false
      }
    },
    formatDate (d) {
      return format(new Date(d), 'dd.MM.yyyy HH:mm:ss')
    }
  }
}
</script>
