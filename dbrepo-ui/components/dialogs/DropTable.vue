<template>
  <div>
    <v-form ref="form" v-model="valid" autocomplete="off" @submit.prevent="submit">
      <v-card
        :title="$t('pages.table.subpages.drop.title') + ' ' + table.internal_name"
        variant="elevated">
        <v-card-text>
          <v-row dense>
            <v-col>
              <span v-text="$t('pages.table.subpages.drop.warning.prefix')" />
              &nbsp;<code class="code-key">{{ table.internal_name }}</code>&nbsp;
              <span v-text="$t('pages.table.subpages.drop.warning.suffix')" />
            </v-col>
          </v-row>
          <v-row>
            <v-col>
              <v-text-field
                id="confirm"
                v-model="confirm"
                name="confirm"
                persistent-hint
                :variant="inputVariant"
                :label="$t('pages.table.subpages.drop.name.label')"
                :hint="$t('pages.table.subpages.drop.name.hint')"
                autofocus
                required />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
            :variant="buttonVariant"
            :text="$t('navigation.cancel')"
            @click="cancel" />
          <v-btn
            color="error"
            variant="flat"
            :text="$t('navigation.delete')"
            :loading="loadingDelete"
            :disabled="confirm !== table.internal_name"
            type="submit"
            @click="dropTable" />
        </v-card-actions>
      </v-card>
    </v-form>
  </div>
</template>

<script>
import { useCacheStore } from '@/stores/cache'

export default {
  data () {
    return {
      confirm: null,
      loadingDelete: false,
      valid: false,
      cacheStore: useCacheStore()
    }
  },
  computed: {
    table () {
      return this.cacheStore.getTable
    },
    database () {
      return this.cacheStore.getDatabase
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
    submit () {
      this.$refs.form.validate()
    },
    cancel () {
      this.$emit('close', { action: 'closed' })
    },
    dropTable () {
      if (!this.table.id) {
        return
      }
      this.loadingDelete = true
      const tableService = useTableService()
      tableService.remove(this.database.id, this.table.id)
        .then(() => {
          console.info('Deleted table with id ', this.table.id)
          this.cacheStore.reloadDatabase()
          const toast = useToastInstance()
          toast.success('Successfully deleted table with id ' + this.table.id)
          this.$router.push(`/database/${this.$route.params.database_id}/table`)
        })
        .finally(() => {
          this.loadingDelete = false
        })
    }
  }
}
</script>
<style scoped>
.code-key {
  padding: 2px 4px;
}
</style>
