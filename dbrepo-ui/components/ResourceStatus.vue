<template>
  <span
    v-if="mode">
    <v-chip
      v-if="!inline"
      :size="size"
      :color="color"
      variant="outlined">
      {{ status }}
    </v-chip>
    <span
      v-else>
      {{ status }}
    </span>
  </span>
</template>

<script>
export default {
  props: {
    resource: {
      default: () => {
        return null
      }
    },
    inline: {
      default: () => {
        return false
      }
    },
    size: {
      default: () => {
        return 'small'
      }
    }
  },
  computed: {
    mode () {
      if (!this.resource) {
        return null
      }
      if (!this.resource.is_public && !this.resource.is_schema_public) {
        return 'draft'
      } else if(!this.resource.is_public && this.resource.is_schema_public) {
        return 'schema'
      } else if(this.resource.is_public && !this.resource.is_schema_public) {
        return 'data'
      }
      return 'public'
    },
    status () {
      if (!this.resource) {
        return null
      }
      return this.$t(`pages.database.status.${this.mode}`)
    },
    hasIdentifier () {
      if (this.resource.identifiers?.length === 0) {
        return false
      }
      return this.resource.identifiers.filter(i => i.status === 'published').length > 0
    },
    color () {
      if (this.hasIdentifier) {
        return 'info'
      }
      switch (this.mode) {
        case 'schema':
        case 'data':
          return 'warning'
        case 'draft':
          return 'error'
        case 'public':
          return 'success'
      }
    }
  }
}
</script>
