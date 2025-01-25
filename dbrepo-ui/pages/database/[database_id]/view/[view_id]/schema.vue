<template>
  <div
    v-if="canViewSchema">
    <ViewToolbar />
    <v-toolbar
      color="secondary"
      :title="$t('pages.table.subpages.schema.title')"
      variant="flat" />
    <v-card
      variant="flat"
      rounded="0"
      tile>
      <v-data-table
        v-if="view"
        class="full-width"
        disable-sort
        :loading="loading"
        hide-default-footer
        :items-per-page="-1"
        :headers="headers"
        :items="view.columns">
        <template v-slot:item.is_null_allowed="{ item }">
          <span
            v-if="item.is_null_allowed">
            {{ $t('pages.table.subpages.schema.bullet') }}
          </span>
          {{ item.is_null_allowed }}
        </template>
        <template v-slot:item.extra="{ item }">
          <pre>{{ extra(item) }}</pre>
        </template>
        <template v-slot:item.column_concept="{ item }">
          <a
            v-if="hasConcept(item)"
            :href="item.concept.uri">
            {{ item.concept.name ? item.concept.name : item.concept.uri }}
          </a>
        </template>
        <template v-slot:item.column_unit="{ item }">
          <a
            v-if="hasUnit(item)"
            :href="item.unit.uri">
            {{ item.unit.name ? item.unit.name : item.unit.uri }}
          </a>
        </template>
      </v-data-table>
    </v-card>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script>
import TableToolbar from '@/components/table/TableToolbar.vue'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      loading: false,
      items: [
        {
          title: this.$t('navigation.databases'),
          to: '/database'
        },
        {
          title: `${this.$route.params.database_id}`,
          to: `/database/${this.$route.params.database_id}/info`
        },
        {
          title: this.$t('navigation.views'),
          to: `/database/${this.$route.params.database_id}/view`
        },
        {
          title: `${this.$route.params.view_id}`,
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}`
        },
        {
          title: this.$t('navigation.schema'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/schema`,
          disabled: true
        }
      ],
      headers: [
        { value: 'internal_name', title: this.$t('pages.table.subpages.schema.internal-name.title') },
        { value: 'type', title: this.$t('pages.table.subpages.schema.column-type.title') },
        { value: 'extra', title: this.$t('pages.table.subpages.schema.extra.title') },
        { value: 'column_concept', title: this.$t('pages.table.subpages.schema.concept.title') },
        { value: 'column_unit', title: this.$t('pages.table.subpages.schema.unit.title') },
        { value: 'is_null_allowed', title: this.$t('pages.table.subpages.schema.nullable.title') },
        { value: 'description', title: this.$t('pages.table.subpages.schema.description.title') },
      ],
      cacheStore: useCacheStore()
    }
  },
  computed: {
    database () {
      return this.cacheStore.getDatabase
    },
    view () {
      return this.cacheStore.getView
    },
    access () {
      return this.cacheStore.getAccess
    },
    roles () {
      return this.cacheStore.getRoles
    },
    cacheUser () {
      return this.cacheStore.getUser
    },
    canViewSchema () {
      if (!this.view) {
        return false
      }
      if (this.view.is_schema_public) {
        return true
      }
      if (!this.access) {
        return false
      }
      const userService = useUserService()
      return userService.hasReadAccess(this.access)
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
  methods: {
    extra (column) {
      if (column.type === 'float') {
        return `precision=${column.size}`
      } else if (['decimal', 'double'].includes(column.type)) {
        let extra = ''
        if (column.size !== null) {
          extra += `size=${column.size}`
        }
        if (column.d !== null) {
          if (extra.length > 0) {
            extra += ', '
          }
          extra += `d=${column.d}`
        }
        return extra
      } else if (column.type === 'enum') {
        return `(${column.enums.join(', ')})`
      } else if (column.type === 'set') {
        return `(${column.sets.join(', ')})`
      } else if (['int', 'char', 'varchar', 'binary', 'varbinary', 'tinyint', 'size="small"int', 'mediumint', 'bigint'].includes(column.type)) {
        return column.size !== null ? `size=${column.size}` : ''
      }
      return null
    },
    hasUnit (item) {
      return item.unit && 'uri' in item.unit
    },
    hasConcept (item) {
      return item.concept && 'uri' in item.concept
    }
  }
}
</script>
