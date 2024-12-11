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
import { useUserStore } from '@/stores/user'
import { useCacheStore } from '@/stores/cache'

export default {
  components: {
    TableToolbar
  },
  data () {
    return {
      loading: false,
      view: null,
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
        { value: 'column_type', title: this.$t('pages.table.subpages.schema.column-type.title') },
        { value: 'extra', title: this.$t('pages.table.subpages.schema.extra.title') },
        { value: 'column_concept', title: this.$t('pages.table.subpages.schema.concept.title') },
        { value: 'column_unit', title: this.$t('pages.table.subpages.schema.unit.title') },
        { value: 'is_null_allowed', title: this.$t('pages.table.subpages.schema.nullable.title') },
        { value: 'description', title: this.$t('pages.table.subpages.schema.description.title') },
      ],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  mounted () {
    this.fetchView()
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    access () {
      return this.userStore.getAccess
    },
    hasReadAccess () {
      if (!this.access) {
        return false
      }
      return this.access.type === 'read' || this.access.type === 'write_all' || this.access.type === 'write_own'
    },
    cachedView () {
      if (!this.database) {
        return null
      }
      return this.database.views.filter(v => v.id === Number(this.$route.params.view_id))[0]
    },
    canViewSchema () {
      if (!this.cachedView) {
        return false
      }
      if (this.cachedView.is_schema_public) {
        return true
      }
      if (!this.user) {
        return false
      }
      return this.hasReadAccess || this.cachedView.owned_by === this.user.id || this.database.owner.id === this.user.id
    },
    roles () {
      return this.userStore.getRoles
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
      if (column.column_type === 'float') {
        return `precision=${column.size}`
      } else if (['decimal', 'double'].includes(column.column_type)) {
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
      } else if (column.column_type === 'enum') {
        return `(${column.enums.join(', ')})`
      } else if (column.column_type === 'set') {
        return `(${column.sets.join(', ')})`
      } else if (['int', 'char', 'varchar', 'binary', 'varbinary', 'tinyint', 'size="small"int', 'mediumint', 'bigint'].includes(column.column_type)) {
        return column.size !== null ? `size=${column.size}` : ''
      }
      return null
    },
    hasUnit (item) {
      return item.unit && 'uri' in item.unit
    },
    hasConcept (item) {
      return item.concept && 'uri' in item.concept
    },
    fetchView () {
      this.loading = true
      const viewService = useViewService()
      viewService.findOne(this.$route.params.database_id, this.$route.params.view_id)
        .then((view) => {
          this.view = view
          this.loading = false
        })
        .catch(({code}) => {
          this.loading = false
          const toast = useToastInstance()
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
