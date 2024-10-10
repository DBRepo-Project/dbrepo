<template>
  <div>
    <v-card>
      <v-card-title>
        {{ entity.name }}
      </v-card-title>
      <v-card-subtitle>
        <a
          :href="entity.uri"
          target="_blank">
          {{ entity.uri }}
        </a>
      </v-card-subtitle>
      <v-card-text>
        <p>
          {{ description }}
        </p>
      </v-card-text>
      <div
        v-for="(item,idx) in entity.columns"
        :key="idx">
        <v-list-item two-line :to="link(item)">
          <v-list-item-title>
            {{ item.name }}
          </v-list-item-title>
          <v-list-item-subtitle
            class="mt-2">
            {{ link(item) }}
          </v-list-item-subtitle>
        </v-list-item>
      </div>
      <v-card-actions>
        <v-spacer />
        <v-btn
          :variant="buttonVariant"
          :text="$t('navigation.cancel')"
          @click="cancel" />
      </v-card-actions>
    </v-card>
  </div>
</template>

<script>
export default {
  props: {
    mode: {
      type: String,
      default () {
        return 'unit'
      }
    },
    entity: {
      type: Object,
      default () {
        return {}
      }
    }
  },
  data () {
    return {
    }
  },
  computed: {
    description () {
      if (!this.entity.description) {
        return '(no description)'
      }
      return this.entity.description
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
    cancel () {
      this.$emit('close', { success: false, action: 'cancel' })
    },
    link (item) {
      return `/database/${item.database_id}/table/${item.table_id}/schema`
    }
  }
}
</script>
<style scoped>
</style>
