<template>
  <div>
    <DBToolbar />
    <v-progress-linear v-if="loading" />
    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-card flat>
          <v-card-text>
            <v-list dense>
              <v-list-item>
                <v-list-item-content>
                  <v-list-item-title>
                    Visibility
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ database.is_public ? 'Public' : 'Private' }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Publisher
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ publisher }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Description
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="paragraph" width="50%" />
                    <span v-if="!loading">{{ description }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Creator
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ creator }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    Language
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ language }}</span>
                  </v-list-item-content>
                  <v-list-item-title class="mt-2">
                    License
                  </v-list-item-title>
                  <v-list-item-content>
                    <v-skeleton-loader v-if="loading" type="text" class="skeleton-small" />
                    <span v-if="!loading">{{ license }}</span>
                  </v-list-item-content>
                </v-list-item-content>
              </v-list-item>
            </v-list>
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
      database: {
        id: null,
        name: null,
        description: null,
        is_public: null,
        publisher: null,
        created: null,
        language: null,
        license: null,
        creator: {
          titles_before: null,
          firstname: null,
          lastname: null,
          username: null,
          titles_after: null
        }
      },
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
    description () {
      return this.database.description === null ? '(no description)' : this.database.description
    },
    publisher () {
      return this.database.publisher === null ? '(none)' : this.database.publisher
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
    },
    language () {
      return this.database.language === null ? '(none)' : this.database.language
    },
    license () {
      return this.database.license === null ? '(none)' : this.database.license
    },
    created () {
      return format(new Date(this.database.created), 'dd.MM.yyyy HH:mm:ss')
    },
    creator () {
      if (this.database.creator.firstname && this.database.creator.lastname) {
        let creator = ''
        if (this.database.creator.titles_before) {
          creator += (this.database.creator.titles_before + ' ')
        }
        creator += (this.database.creator.firstname + ' ' + this.database.creator.lastname)
        if (this.database.creator.titles_after) {
          creator += (this.database.creator.titles_after + ' ')
        }
        return creator
      }
      return this.database.creator.username
    }
  },
  mounted () {
    this.loadDatabase()
  },
  methods: {
    async loadDatabase () {
      this.loading = true
      try {
        const res = await this.$axios.get(`/api/container/${this.$route.params.container_id}/database/${this.$route.params.database_id}`, this.config)
        this.database = res.data
        console.debug('database', res.data)
      } catch (err) {
        this.$toast.error('Could not load database.')
      }
      this.loading = false
    },
    formatDate (d) {
      return format(new Date(d), 'dd.MM.yyyy HH:mm:ss')
    }
  }
}
</script>
<style>
.v-card__text {
  font-size: initial;
}
.skeleton-small .v-skeleton-loader__text {
  width: 100px;
}
</style>
