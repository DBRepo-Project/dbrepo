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
      if (!this.resource.is_public) {
        if (!this.resource.is_schema_public) {
          return 'draft'
        }
        return 'private'
      }
      if (!this.resource.is_schema_public) {
        return 'private'
      }
      return 'public'
    },
    status () {
      if (!this.resource) {
        return null
      }
      return this.$t(`pages.database.status.${this.mode}`)
    },
    color () {
      switch (this.mode) {
        case 'private':
          return 'secondary'
        case 'draft':
          return 'warning'
        case 'public':
          return 'success'
      }
    }
  }
}
</script>
