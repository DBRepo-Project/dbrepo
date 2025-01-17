<template>
  <div
    v-if="canViewSettings">
    <ViewToolbar />
    <v-form
      v-if="canUpdateVisibility"
      ref="form"
      v-model="valid"
      autocomplete="off"
      @submit.prevent="submit">
      <v-card
        variant="flat"
        rounded="0"
        :title="$t('pages.view.settings.title')"
        :subtitle="$t('pages.view.settings.subtitle')">
        <v-card-text>
          <v-row
            dense>
            <v-col
              md="4">
              <v-select
                v-model="modify.is_public"
                :items="dataOptions"
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.data.label')"
                :hint="$t('pages.database.resource.data.hint', { resource: 'view' })" />
            </v-col>
            <v-col
              md="4">
              <v-select
                v-model="modify.is_schema_public"
                :items="schemaOptions"
                persistent-hint
                :variant="inputVariant"
                required
                :rules="[
                  v => v !== null || $t('validation.required')
                ]"
                :label="$t('pages.database.resource.schema.label')"
                :hint="$t('pages.database.resource.schema.hint', { resource: 'view', schema: 'query' })" />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-btn
                variant="flat"
                size="small"
                :disabled="!valid || !isChange"
                :color="buttonColor"
                :loading="loading"
                type="submit"
                :text="$t('navigation.modify')"
                @click="update" />
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-form>
    <v-divider
      v-if="canDeleteView" />
    <v-card
      v-if="canDeleteView"
      variant="flat"
      rounded="0"
      :title="$t('pages.view.delete.title')"
      :subtitle="$t('pages.view.delete.subtitle', { view: view.internal_name })">
      <v-card-text>
        <v-row>
          <v-col
            md="8">
            <v-btn
              size="small"
              variant="flat"
              color="error"
              @click="askDelete">
              {{ $t('navigation.delete')}}
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
    <v-breadcrumbs
      :items="items"
      class="pa-0 mt-2" />
  </div>
</template>

<script>
import ViewToolbar from '@/components/view/ViewToolbar.vue'
import { useUserStore } from '@/stores/user.js'
import { useCacheStore } from '@/stores/cache.js'

export default {
  components: {
    ViewToolbar
  },
  data () {
    return {
      valid: false,
      loading: false,
      modify: {
        is_public: null,
        is_schema_public: null
      },
      dataOptions: [
        { title: this.$t('pages.database.resource.data.enabled'), value: true },
        { title: this.$t('pages.database.resource.data.disabled'), value: false },
      ],
      schemaOptions: [
        { title: this.$t('pages.database.resource.schema.enabled'), value: true },
        { title: this.$t('pages.database.resource.schema.disabled'), value: false },
      ],
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
          title: this.$t('navigation.settings'),
          to: `/database/${this.$route.params.database_id}/view/${this.$route.params.view_id}/settings`,
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
      dateColumns: [],
      userStore: useUserStore(),
      cacheStore: useCacheStore()
    }
  },
  computed: {
    user () {
      return this.userStore.getUser
    },
    database () {
      return this.cacheStore.getDatabase
    },
    view () {
      return this.cacheStore.getView
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
    roles () {
      return this.userStore.getRoles
    },
    isChange () {
      if (!this.view) {
        return false
      }
      if (this.view.is_public !== this.modify.is_public) {
        return true
      }
      return this.view.is_schema_public !== this.modify.is_schema_public
    },
    canUpdateVisibility () {
      if (!this.roles || !this.user || !this.view) {
        return false
      }
      return this.roles.includes('modify-view-visibility') && this.view.owner.id === this.user.id
    },
    canDeleteView () {
      if (!this.roles || !this.user || !this.view) {
        return false
      }
      return this.roles.includes('delete-database-view') && this.view.owner.id === this.user.id
    },
    canViewSettings () {
      if (!this.user || !this.view) {
        return false
      }
      return this.view.owner.id === this.user.id
    },
    inputVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.input.contrast : runtimeConfig.public.variant.input.normal
    },
    buttonVariant () {
      const runtimeConfig = useRuntimeConfig()
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast') ? runtimeConfig.public.variant.button.contrast : runtimeConfig.public.variant.button.normal
    },
    buttonColor () {
      return !this.isChange ? null : 'warning'
    }
  },
  mounted() {
    if (!this.view) {
      return
    }
    this.modify.is_public = this.view.is_public
    this.modify.is_schema_public = this.view.is_schema_public
    this.modify.description = this.view.description
  },
  methods: {
    submit () {
      this.$refs.form.validate()
    },
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
    closed (event) {
      const { success } = event
      console.debug('closed dialog', event)
      if (success) {
        const toast = useToastInstance()
        toast.success(this.$t('success.table.semantics'))
        this.cacheStore.reloadTable()
      }
      this.dialogSemantic = false
    },
    askDelete () {
      if (!confirm(this.$t('pages.view.delete.subtitle', { view: this.view.internal_name }))) {
        return
      }
      this.loadingDelete = true
      const viewService = useViewService()
      viewService.remove(this.database.id, this.view.id)
        .then(() => {
          console.info('Deleted view with id ', this.view.id)
          this.cacheStore.reloadDatabase()
          const toast = useToastInstance()
          toast.success('Successfully deleted view with id ' + this.view.id)
          this.$router.push(`/database/${this.$route.params.database_id}/view`)
        })
        .catch(({code, message}) => {
          const toast = useToastInstance()
          if (typeof code !== 'string') {
            return
          }
          toast.error(this.$t(code))
        })
        .finally(() => {
          this.loadingDelete = false
        })
    },
    update () {
      this.loading = true
      const viewService = useViewService()
      viewService.update(this.$route.params.database_id, this.$route.params.view_id, this.modify)
        .then(() => {
          this.loading = false
          const toast = useToastInstance()
          toast.success(this.$t('success.view.modified'))
          this.cacheStore.reloadView()
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
