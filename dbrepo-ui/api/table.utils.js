class TableUtils {
  isOwner (table, user) {
    if (!table || !user) {
      return false
    }
    return table.owner.id === user.id
  }
}

export default new TableUtils()
