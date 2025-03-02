<template>
  <v-card
    v-if="container"
    variant="flat"
    rounded="0">
    <v-divider class="mx-4" />
    <v-card-title>
      {{ container.name }}
    </v-card-title>
    <v-card-subtitle>
      {{ $t('pages.container.subtitle.text') }}
    </v-card-subtitle>
    <v-card-text>
      <v-progress-linear
        v-model="utilization"
        :color="colorVariant"
        height="20"
        class="font-small">
        <template v-slot:default>
          {{ container.count }} / {{ quota }}
        </template>
      </v-progress-linear>
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  data() {
    return {
      loading: false
    }
  },
  props: {
    container: {
      default: () => {
        return null
      }
    }
  },
  computed: {
    quota () {
      if (!this.container || !this.container.quota) {
        return '∞'
      }
      return this.container.quota
    },
    utilization () {
      if (!this.container || !this.container.quota) {
        return 0
      }
      return this.container.count * 100.0 / this.container.quota
    },
    colorVariant () {
      return this.isContrastTheme ? '' : (this.isDarkTheme ? 'tertiary' : 'secondary')
    },
    isContrastTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().endsWith('contrast')
    },
    isDarkTheme () {
      return this.$vuetify.theme.global.name.toLowerCase().startsWith('dark')
    }
  }
}
</script>
