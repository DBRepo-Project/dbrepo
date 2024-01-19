<template>
  <div>
    <v-progress-linear v-if="loading" :color="loadingColor" indeterminate />
    <v-card v-if="!loading && views.length === 0" flat>
      <v-card-text v-text="emptyText" />
    </v-card>
    <div v-for="(view,i) in views" :key="i">
      <v-divider v-if="i !== 0" class="mx-4" />
      <v-list-item-group>
        <v-list-item two-line :class="clazz(view)" :to="`/database/${$route.params.database_id}/view/${view.id}/info`">
          <v-list-item-content>
            <v-list-item-title v-text="view.name" />
            <v-list-item-subtitle class="mt-2">
              <pre>{{ view.query }}</pre>
            </v-list-item-subtitle>
          </v-list-item-content>
          <v-list-item-action v-if="hasIdentifiers(view)">
            <v-tooltip left>
              <template v-slot:activator="{ on, attrs }">
                <v-icon color="primary" v-bind="attrs" v-on="on">mdi-identifier</v-icon>
              </template>
              Persistent identifier
            </v-tooltip>
          </v-list-item-action>
        </v-list-item>
      </v-list-item-group>
    </div>
  </div>
</template>

<script>
export default {
  data () {
    return {
      loading: false,
      loadingDetails: false,
      error: false
    }
  },
  computed: {
    loadingColor () {
      return this.error ? 'red lighten-2' : 'primary'
    },
    user () {
      return this.$store.state.user
    },
    database () {
      return this.$store.state.database
    },
    emptyText () {
      const add = this.database && this.database.is_public ? '' : ' public'
      return `(no${add} views)`
    },
    views () {
      if (!this.database) {
        return []
      }
      return this.database.views
    }
  },
  mounted () {
  },
  methods: {
    clazz (view) {
      return this.hasIdentifiers(view) ? 'primary--text' : null
    },
    hasIdentifiers (view) {
      return view && 'identifiers' in view && view.identifiers.length > 0
    }
  }
}
</script>

<style>
.colTable thead th {
  text-align: initial;
}
.colTable tbody tr td {
  padding-left: 0;
}
.align-right {
  text-align: right;
}
.full-width {
  width: 100%;
}
.amqp-consumer {
  display: inline;
}
</style>
