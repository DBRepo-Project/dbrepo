class UserUtils {
  hasReadAccess (access) {
    if (!access) {
      return false
    }
    return access.type === 'read' || access.type === 'write_own' || access.type === 'write_all'
  }

  hasWriteAccess (table, access, user) {
    if (!table || !access) {
      return false
    }
    if (access.type === 'write_all') {
      return true
    }
    return access.type === 'write_own' && table.owner.id === user.id
  }
}

export default new UserUtils()
