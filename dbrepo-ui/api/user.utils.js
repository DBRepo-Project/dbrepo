class UserUtils {
  hasReadAccess (access) {
    if (!access) {
      return false
    }
    return access.type === 'read' || this.hasWriteAccess(access)
  }

  hasWriteAccess (access) {
    if (!access) {
      return false
    }
    return access.type === 'write_own' || access.type === 'write_all'
  }
}

export default new UserUtils()
