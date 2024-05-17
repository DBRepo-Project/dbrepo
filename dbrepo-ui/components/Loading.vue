<template>
  <v-list-item-title>
    {{ $t('navigation.loading') }}
    <v-progress-circular
      color="primary"
      size="24"
      indeterminate />
  </v-list-item-title>
</template>
<script>
export default {
  props: {
    loading: {
      type: Boolean,
      default: () => {
        return true
      }
    }
  }
}
</script>
