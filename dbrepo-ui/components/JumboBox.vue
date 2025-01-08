<template>
  <div>
    <v-row>
      <v-col
        offset-md="2"
        md="8">
        <v-card
          class="mx-auto"
          :title="title"
          :subtitle="subtitle"
          :text="text">
          <template v-slot:prepend>
            <v-icon
              :color="iconColor">
              {{ icon }}
            </v-icon>
          </template>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
<script>
export default {
  props: {
    title: {
      type: String,
      default: () => {
        return 'Title'
      }
    },
    subtitle: {
      type: String,
      default: () => {
        return 'Subtitle'
      }
    },
    text: {
      type: String,
      default: () => {
        return 'Text'
      }
    },
    icon: {
      type: String,
      default: () => {
        return 'mdi-alert-rhombus-outline'
      }
    },
    iconColor: {
      type: String,
      default: () => {
        return 'warning'
      }
    }
  }
}
</script>
