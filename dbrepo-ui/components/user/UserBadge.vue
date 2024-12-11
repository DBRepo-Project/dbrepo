<template>
  <p class="mb-0">
    <OrcidIcon
      v-if="hasOrcid"
      class="mr-1"
      :orcid="orcid" />
    <span v-if="isSelf">
      <v-badge
        inline
        content="you"
        color="code">
        {{ creatorName }}
      </v-badge>
    </span>
    <span
      v-else>
      {{ creatorName }}
    </span>
  </p>
</template>

<script>
import OrcidIcon from '@/components/icons/OrcidIcon.vue'

export default {
  components: {
    OrcidIcon
  },
  props: {
    user: null,
    otherUser: null
  },
  computed: {
    hasOrcid () {
      return !(!this.user || !this.user.attributes || !this.user.attributes.orcid);
    },
    orcid () {
      if (!this.hasOrcid) {
        return null
      }
      return this.user.attributes.orcid
    },
    creatorName () {
      const userService = useUserService()
      return userService.userToFullName(this.user)
    },
    isSelf () {
      if (!this.otherUser || !this.user) {
        return false
      }
      return this.user.id === this.otherUser.id
    }
  }
}
</script>
